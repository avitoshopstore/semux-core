/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.integration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.semux.core.Amount.Unit.SEM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semux.IntegrationTest;
import org.semux.Kernel;
import org.semux.Kernel.State;
import org.semux.KernelMock;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.Genesis;
import org.semux.net.NodeManager;
import org.semux.net.NodeManager.Node;
import org.semux.net.SemuxChannelInitializer;
import org.semux.rules.KernelRule;

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Genesis.class, NodeManager.class })
@PowerMockIgnore({ "jdk.internal.*", "javax.management.*" })
public class SyncingTest {

    private static final Amount PREMINE = SEM.of(5000);

    @Rule
    KernelRule kernelRule1 = new KernelRule(51610, 51710);

    @Rule
    KernelRule kernelRule2 = new KernelRule(51620, 51720);

    @Rule
    KernelRule kernelRule3 = new KernelRule(51630, 51730);

    @Rule
    KernelRule kernelRule4 = new KernelRule(51640, 51740);

    private KernelMock kernel1; // seed node
    private KernelMock kernel2; // seed node
    private KernelMock kernel3; // seed node
    private KernelMock kernel4; // normal node

    protected int targetHeight() {
        return 2;
    }

    @Before
    public void setUp() throws Exception {
        // prepare kernels
        kernelRule1.speedUpConsensus();
        kernelRule2.speedUpConsensus();
        kernelRule3.speedUpConsensus();
        kernelRule4.speedUpConsensus();
        kernel1 = kernelRule1.getKernel();
        kernel2 = kernelRule2.getKernel();
        kernel3 = kernelRule3.getKernel();
        kernel4 = kernelRule4.getKernel();

        beforeStart();

        // start kernels
        kernel1.start();
        kernel2.start();
        kernel3.start();
        kernel4.start();

        List<Kernel> kernels = new ArrayList<>();
        kernels.add(kernel1);
        kernels.add(kernel2);
        kernels.add(kernel3);

        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(kernel1.getConfig().p2pListenIp(), kernel1.getConfig().p2pListenPort()));
        nodes.add(new Node(kernel2.getConfig().p2pListenIp(), kernel2.getConfig().p2pListenPort()));
        nodes.add(new Node(kernel3.getConfig().p2pListenIp(), kernel3.getConfig().p2pListenPort()));

        // Make the three kernels connect
        for (int i = 0; i < kernels.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                // Note: with the new three-way handshake, two nodes can't connect to each other
                // at the same time.
                SemuxChannelInitializer ci = new SemuxChannelInitializer(kernels.get(i), nodes.get(j));
                kernels.get(i).getClient().connect(nodes.get(j), ci);
            }
        }

        // let the fourth kernel connects
        kernel4.getNodeManager().addNodes(nodes);

        // wait for kernels
        await().atMost(20, SECONDS).until(() -> kernel1.state() == State.RUNNING
                && kernel2.state() == State.RUNNING
                && kernel3.state() == State.RUNNING
                && kernel4.state() == State.RUNNING
                && kernel4.getChannelManager().getActivePeers().size() >= 3);
    }

    protected void beforeStart() {
        // mock genesis.json
        Genesis genesis = mockGenesis();
        mockStatic(Genesis.class);
        when(Genesis.load(any())).thenReturn(genesis);
    }

    @After
    public void tearDown() {
        // stop kernels
        kernel1.stop();
        kernel2.stop();
        kernel3.stop();
        kernel4.stop();
    }

    @Test
    public void testSync() throws IOException {
        // validators has forged the n-th block
        await().atMost(60, SECONDS).until(() -> kernel1.getBlockchain().getLatestBlockNumber() >= targetHeight()
                && kernel2.getBlockchain().getLatestBlockNumber() >= targetHeight()
                && kernel3.getBlockchain().getLatestBlockNumber() >= targetHeight());

        // normal node can sync to the same height
        await().atMost(20, SECONDS).until(() -> kernel4.getBlockchain().getLatestBlockNumber() >= targetHeight());

        // check block and votes
        for (int i = 1; i <= targetHeight(); i++) {
            Block block = kernel4.getBlockchain().getBlock(i);
            Block previousBlock = kernel4.getBlockchain().getBlock(i - 1);
            assertTrue(Block.validateHeader(previousBlock.getHeader(), block.getHeader()));
            assertTrue(Block.validateTransactions(previousBlock.getHeader(), block.getTransactions(),
                    kernel4.getConfig().network()));
            assertTrue(Block.validateResults(previousBlock.getHeader(), block.getResults()));

            assertTrue(block.getVotes().size() >= 3 * 2 / 3);
        }
    }

    protected Genesis mockGenesis() {
        // mock premine
        List<Genesis.Premine> premines = new ArrayList<>();
        premines.add(new Genesis.Premine(kernel4.getCoinbase().toAddress(), PREMINE, ""));

        // mock delegates
        HashMap<String, String> delegates = new HashMap<>();
        delegates.put("delegate1", kernel1.getCoinbase().toAddressString());
        delegates.put("delegate2", kernel2.getCoinbase().toAddressString());
        delegates.put("delegate3", kernel3.getCoinbase().toAddressString());

        // mock genesis
        return Genesis.jsonCreator(0,
                "0x0000000000000000000000000000000000000000",
                "0x0000000000000000000000000000000000000000000000000000000000000000",
                1504742400000L,
                "semux",
                premines,
                delegates,
                new HashMap<>());
    }
}
