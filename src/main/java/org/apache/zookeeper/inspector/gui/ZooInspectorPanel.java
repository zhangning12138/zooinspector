/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zookeeper.inspector.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;

import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.inspector.gui.nodeviewer.NodeViewerData;
import org.apache.zookeeper.inspector.gui.nodeviewer.ZooInspectorNodeViewer;
import org.apache.zookeeper.inspector.logger.LoggerFactory;
import org.apache.zookeeper.inspector.manager.ZooInspectorManager;

/**
 * The parent {@link JPanel} for the whole application
 */
public class ZooInspectorPanel extends JPanel implements
        NodeViewersChangeListener {
    private final JButton refreshButton;
    private final JButton disconnectButton;
    private final JButton connectButton;
    private final ZooInspectorNodeViewersPanel nodeViewersPanel;
    private final ZooInspectorTreeViewer treeViewer;
    private final ZooInspectorManager zooInspectorManager;
    private final JButton addNodeButton;
    private final JButton deleteNodeButton;
    private final JButton nodeViewersButton;
    private final JButton aboutButton;
    private final JButton searchButton;
    private final List<NodeViewersChangeListener> listeners = new ArrayList<NodeViewersChangeListener>();
    {
        listeners.add(this);
    }

    public boolean checkZookeeperStates(String info) {
      if (zooInspectorManager == null
          || zooInspectorManager.getZookeeperStates() != States.CONNECTED) {
        refreshButton.setEnabled(false);
        addNodeButton.setEnabled(false);
        deleteNodeButton.setEnabled(false);
        JOptionPane
        .showMessageDialog(
                ZooInspectorPanel.this,
                info,
                "Error", JOptionPane.ERROR_MESSAGE);
        return false;
      }
      return true;
    }

    /**
     * @param zooInspectorManager
     *            - the {@link ZooInspectorManager} for the application
     */
    public ZooInspectorPanel(final ZooInspectorManager zooInspectorManager) {
        this.zooInspectorManager = zooInspectorManager;
        final ArrayList<ZooInspectorNodeViewer> nodeViewers = new ArrayList<ZooInspectorNodeViewer>();
        try {
            List<String> defaultNodeViewersClassNames = this.zooInspectorManager
                    .getDefaultNodeViewerConfiguration();
            for (String className : defaultNodeViewersClassNames) {
                nodeViewers.add((ZooInspectorNodeViewer) Class.forName(
                        className).newInstance());
            }
        } catch (Exception ex) {
            LoggerFactory.getLogger().error(
                    "加载默认节点查看器时出错。", ex);
            JOptionPane.showMessageDialog(ZooInspectorPanel.this,
                    "加载默认节点查看器时出错：" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        this.setLayout(new BorderLayout());
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        connectButton = new JButton(ZooInspectorIconResources.getConnectIcon());
        disconnectButton = new JButton(ZooInspectorIconResources
                .getDisconnectIcon());
        refreshButton = new JButton(ZooInspectorIconResources.getRefreshIcon());
        addNodeButton = new JButton(ZooInspectorIconResources.getAddNodeIcon());
        deleteNodeButton = new JButton(ZooInspectorIconResources
                .getDeleteNodeIcon());
        nodeViewersButton = new JButton(ZooInspectorIconResources
                .getChangeNodeViewersIcon());
        aboutButton = new JButton(ZooInspectorIconResources
                .getInformationIcon());
        searchButton = new JButton(ZooInspectorIconResources
                .getSearchIcon());
        nodeViewersPanel = new ZooInspectorNodeViewersPanel(
                zooInspectorManager, nodeViewers);
        treeViewer = new ZooInspectorTreeViewer(ZooInspectorPanel.this, zooInspectorManager, nodeViewersPanel, searchButton);
        toolbar.add(connectButton);
        toolbar.add(disconnectButton);
        toolbar.add(refreshButton);
        toolbar.add(searchButton);
        toolbar.add(addNodeButton);
        toolbar.add(deleteNodeButton);
        toolbar.add(nodeViewersButton);
        toolbar.add(aboutButton);
        aboutButton.setEnabled(true);
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        refreshButton.setEnabled(false);
        addNodeButton.setEnabled(false);
        deleteNodeButton.setEnabled(false);
        nodeViewersButton.setEnabled(true);
        searchButton.setEnabled(false);
        nodeViewersButton.setToolTipText("更改节点查看器");
        aboutButton.setToolTipText("关于");
        connectButton.setToolTipText("连接");
        disconnectButton.setToolTipText("断开连接");
        refreshButton.setToolTipText("刷新");
        addNodeButton.setToolTipText("增加节点");
        deleteNodeButton.setToolTipText("删除节点");
        connectButton.addActionListener(e -> {
            ZooInspectorConnectionPropertiesDialog zicpd = new ZooInspectorConnectionPropertiesDialog(
                    zooInspectorManager.getLastConnectionProps(),
                    zooInspectorManager.getConnectionPropertiesTemplate(),
                    ZooInspectorPanel.this);
            zicpd.setLocationRelativeTo(ZooInspectorPanel.this);
            zicpd.setVisible(true);
        });
        disconnectButton.addActionListener(e -> disconnect());
        refreshButton.addActionListener(e -> treeViewer.refreshView());
        addNodeButton.addActionListener(e -> {
            final List<String> selectedNodes = treeViewer
                    .getSelectedNodes();
            if (selectedNodes.size() == 1) {
                String nodeName = JOptionPane.showInputDialog(
                        ZooInspectorPanel.this,
                        "请输入新节点名称",
                        "创建节点", JOptionPane.INFORMATION_MESSAGE);
                if (nodeName != null && nodeName.length() > 0) {
                    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

                        @Override
                        protected Boolean doInBackground() throws Exception {
                            return ZooInspectorPanel.this.zooInspectorManager
                                    .createNode(selectedNodes.get(0),
                                            nodeName);
                        }

                        @Override
                        protected void done() {
//                                treeViewer.refreshView();
                          treeViewer.refreshViewAfterAdd(selectedNodes.get(0), nodeName);
                        }
                    };
                    worker.execute();
                }
            } else {
                JOptionPane.showMessageDialog(ZooInspectorPanel.this,
                        "请为新增节点选择1个父节点");
            }
        });
        deleteNodeButton.addActionListener(e -> {
            final List<String> selectedNodes = treeViewer
                    .getSelectedNodes();
            if (selectedNodes.size() == 0) {
                JOptionPane.showMessageDialog(ZooInspectorPanel.this,
                        "无法删除,未选择节点");
            } else {
                int answer = JOptionPane.showConfirmDialog(
                        ZooInspectorPanel.this,
                        "确认删除选中节点吗?(此操作不可恢复)",
                        "确认删除", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (answer == JOptionPane.YES_OPTION) {
                    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

                        @Override
                        protected Boolean doInBackground() {
                            for (String nodePath : selectedNodes) {
                                boolean result = ZooInspectorPanel.this.zooInspectorManager
                                        .deleteNode(nodePath);
                                if (!result) {
                                  return false;
                                }
                            }
                            return true;
                        }

                        @Override
                        protected void done() {
//                                treeViewer.refreshView();

                          treeViewer.refreshViewAfterDelete(selectedNodes);
                        }
                    };
                    worker.execute();
                }
            }
        });
        nodeViewersButton.addActionListener(e -> {
            ZooInspectorNodeViewersDialog nvd = new ZooInspectorNodeViewersDialog(
                    JOptionPane.getRootFrame(), nodeViewers, listeners,
                    zooInspectorManager);
            nvd.setLocationRelativeTo(ZooInspectorPanel.this);
            nvd.setVisible(true);
        });
        aboutButton.addActionListener(e -> {
            ZooInspectorAboutDialog zicpd = new ZooInspectorAboutDialog(
                    JOptionPane.getRootFrame());
            zicpd.setLocationRelativeTo(ZooInspectorPanel.this);
            zicpd.setVisible(true);
        });

        searchButton.addActionListener(e -> {
            String nodeName = JOptionPane.showInputDialog(
                        ZooInspectorPanel.this,
                        "输入需要查询的节点关键字",
                        "查找接口服务节点", JOptionPane.INFORMATION_MESSAGE);
            if (nodeName != null && nodeName.length() > 0) {
                treeViewer.selectNodeByName(nodeName, ZooInspectorPanel.this);
            }
        });

        JScrollPane treeScroller = new JScrollPane(treeViewer);
        //加快节点树滚动速度
        treeScroller.getVerticalScrollBar().setUnitIncrement(16);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                treeScroller, nodeViewersPanel);
        splitPane.setResizeWeight(0.25);
        this.add(splitPane, BorderLayout.CENTER);
        this.add(toolbar, BorderLayout.NORTH);
    }

    /**
     * @param connectionProps
     *            the {@link Properties} for connecting to the zookeeper
     *            instance
     */
    public void connect(final Properties connectionProps) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() {
                zooInspectorManager.setLastConnectionProps(connectionProps);
                return zooInspectorManager.connect(connectionProps);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        // connect successfully
                        treeViewer.refreshView();
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
                        refreshButton.setEnabled(true);
                        addNodeButton.setEnabled(true);
                        deleteNodeButton.setEnabled(true);
                        searchButton.setEnabled(true);

                        // save successful connect string in default properties
                        zooInspectorManager.updateDefaultConnectionFile(connectionProps);
                    } else {
                        JOptionPane.showMessageDialog(ZooInspectorPanel.this,
                                "无法连接到zookeeper", "错误",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException e) {
                    LoggerFactory
                            .getLogger()
                            .error(
                                    "连接到ZooKeeper服务器时出错",
                                    e);
                } catch (ExecutionException e) {
                    LoggerFactory
                            .getLogger()
                            .error(
                                    "连接到ZooKeeper服务器时出错",
                                    e);
                } catch (IOException e) {
                    LoggerFactory
                      .getLogger()
                      .error("更新默认连接时出错", e);
                }
            }

        };
        worker.execute();
    }

    /**
	 *
	 */
    public void disconnect() {
        disconnect(false);
    }

    /**
     * @param wait
     *            - set this to true if the method should only return once the
     *            application has successfully disconnected
     */
    public void disconnect(boolean wait) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() throws Exception {
                return ZooInspectorPanel.this.zooInspectorManager.disconnect();
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        treeViewer.clearView();
                        connectButton.setEnabled(true);
                        disconnectButton.setEnabled(false);
                        refreshButton.setEnabled(false);
                        addNodeButton.setEnabled(false);
                        deleteNodeButton.setEnabled(false);
                        searchButton.setEnabled(false);
                    }
                } catch (InterruptedException e) {
                    LoggerFactory
                            .getLogger()
                            .error(
                                    "从ZooKeeper服务器断开连接时出错",
                                    e);
                } catch (ExecutionException e) {
                    LoggerFactory
                            .getLogger()
                            .error(
                                    "从ZooKeeper服务器断开连接时出错",
                                    e);
                }
            }

        };
        worker.execute();
        if (wait) {
            while (!worker.isDone()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    LoggerFactory
                            .getLogger()
                            .error(
                                    "从ZooKeeper服务器断开连接时出错",
                                    e);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.apache.zookeeper.inspector.gui.NodeViewersChangeListener#
     * nodeViewersChanged(java.util.List)
     */
    @Override
    public void nodeViewersChanged(List<ZooInspectorNodeViewer> newViewers) {
        this.nodeViewersPanel.setNodeViewers(newViewers);
    }

    /**
     * @param connectionProps
     * @throws IOException
     */
    public void setdefaultConnectionProps(Properties connectionProps)
            throws IOException {
        this.zooInspectorManager.saveDefaultConnectionFile(connectionProps);
    }
}
