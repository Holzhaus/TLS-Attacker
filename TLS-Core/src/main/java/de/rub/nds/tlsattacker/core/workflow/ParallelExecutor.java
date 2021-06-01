/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2021 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsattacker.core.workflow;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.task.ITask;
import de.rub.nds.tlsattacker.core.workflow.task.StateExecutionServerTask;
import de.rub.nds.tlsattacker.core.workflow.task.StateExecutionTask;
import de.rub.nds.tlsattacker.core.workflow.task.TlsTask;

/**
 *
 *
 */
public class ParallelExecutor {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ThreadPoolExecutor executorService;
    private final CompletionService<ITask> completionService;
    private Callable<Integer> timeoutAction;

    private final int size;
    private boolean shouldShutdown = false;

    private final int reexecutions;

    public ParallelExecutor(int size, int reexecutions, ThreadPoolExecutor executorService) {
        this.executorService = executorService;
        completionService = new ExecutorCompletionService<>(executorService);
        this.reexecutions = reexecutions;
        this.size = size;
        if (reexecutions < 0) {
            throw new IllegalArgumentException("Reexecutions is below zero");
        }
    }

    public ParallelExecutor(ThreadPoolExecutor executorService, int reexecutions) {
        this(-1, reexecutions, executorService);
    }

    public ParallelExecutor(int size, int reexecutions) {
        this(size, reexecutions,
            new ThreadPoolExecutor(size, size, 10, TimeUnit.DAYS, new LinkedBlockingDeque<Runnable>()));
    }

    public ParallelExecutor(int size, int reexecutions, ThreadFactory factory) {
        this(size, reexecutions,
            new ThreadPoolExecutor(size, size, 5, TimeUnit.MINUTES, new LinkedBlockingDeque<Runnable>(), factory));
    }

    public Future<ITask> addTask(TlsTask task) {
        if (executorService.isShutdown()) {
            throw new RuntimeException("Cannot add Tasks to already shutdown executor");
        }
        return completionService.submit(task);
    }

    public Future<ITask> addClientStateTask(State state) {
        return addTask(new StateExecutionTask(state, reexecutions));
    }

    public Future<ITask> addServerStateTask(State state, ServerSocket socket) {
        return addTask(new StateExecutionServerTask(state, socket, reexecutions));
    }

    public void bulkExecuteClientStateTasks(List<State> stateList) {
        List<Future> futureList = new LinkedList<>();
        for (State state : stateList) {
            futureList.add(addClientStateTask(state));
        }
        for (Future future : futureList) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException("Failed to execute tasks!", ex);
            }
        }
    }

    public void bulkExecuteClientStateTasks(State... states) {
        this.bulkExecuteClientStateTasks(new ArrayList<>(Arrays.asList(states)));
    }

    public List<ITask> bulkExecuteTasks(List<TlsTask> taskList) {
        List<Future<ITask>> futureList = new LinkedList<>();
        List<ITask> resultList = new ArrayList<>(futureList.size());
        for (TlsTask tlStask : taskList) {
            futureList.add(addTask(tlStask));
        }
        for (Future<ITask> future : futureList) {
            try {
                resultList.add(future.get());
            } catch (InterruptedException | ExecutionException ex) {
                resultList.add(null);
                throw new RuntimeException("Failed to execute tasks!", ex);
            }
        }
        return resultList;
    }

    public List<ITask> bulkExecuteTasks(TlsTask... tasks) {
        return this.bulkExecuteTasks(new ArrayList<>(Arrays.asList(tasks)));
    }

    public int getSize() {
        return size;
    }

    public void shutdown() {
        shouldShutdown = true;
        executorService.shutdown();
    }

    /**
     * Creates a new thread monitoring the completionService. If the last {@link TlsTask} was finished more than 20
     * seconds ago, the function assiged to {@link ParallelExecutor#timeoutAction } is executed.
     *
     * The {@link ParallelExecutor#timeoutAction } function can, for example, try to restart the client/server, so that
     * the remaining {@link TlsTask}s can be finished.
     */
    public void armTimeoutAction() {
        if (timeoutAction == null) {
            LOGGER.warn("No TimeoutAction set, this won't do anything");
            return;
        }

        new Thread(() -> {
            while (!shouldShutdown) {
                try {
                    Future<ITask> task = completionService.poll(20, TimeUnit.SECONDS);
                    if (task != null) {
                        continue;
                    }

                    LOGGER.debug("Timeout");
                    if (executorService.getQueue().size() > 0) {
                        try {
                            int exitCode = timeoutAction.call();
                            if (exitCode != 0) {
                                throw new RuntimeException("TimeoutAction did terminate with code " + exitCode);
                            }
                        } catch (Exception e) {
                            LOGGER.warn("TimeoutAction did not succeed", e);
                        }
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    public int getReexecutions() {
        return reexecutions;
    }

    public Callable<Integer> getTimeoutAction() {
        return timeoutAction;
    }

    public void setTimeoutAction(Callable<Integer> timeoutAction) {
        this.timeoutAction = timeoutAction;
    }
}