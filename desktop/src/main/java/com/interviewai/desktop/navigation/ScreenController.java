package com.interviewai.desktop.navigation;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public abstract class ScreenController {

    private boolean active = true;
    private Task<?> pendingTask;

    protected final boolean isActive() {
        return active;
    }

    protected final void submit(Task<?> task, Executor executor) {
        EventHandler<WorkerStateEvent> success = task.getOnSucceeded();
        EventHandler<WorkerStateEvent> failure = task.getOnFailed();
        task.setOnSucceeded(event -> {
            if (active && success != null) {
                success.handle(event);
            }
        });
        task.setOnFailed(event -> {
            if (active && failure != null) {
                failure.handle(event);
            }
        });
        pendingTask = task;
        try {
            executor.execute(task);
        } catch (RejectedExecutionException busy) {
            task.cancel();
            if (active && failure != null) {
                failure.handle(new WorkerStateEvent(task, WorkerStateEvent.WORKER_STATE_FAILED));
            }
        }
    }

    public void dispose() {
        active = false;
        if (pendingTask != null) {
            pendingTask.cancel(true);
        }
    }
}
