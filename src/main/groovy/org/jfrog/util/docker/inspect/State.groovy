package org.jfrog.util.docker.inspect

/**
 * Created by matankatz on 14/08/2016.
 */
class State {

    private final String STATE = "State"

    String status
    Boolean running
    Boolean paused
    Boolean restarting
    Boolean dead
    Integer pid
    Integer exitCode
    String error
    String startedAt
    String finishedAt

    State init(Map inspect) {
        Map state = inspect.get(STATE)
        this.status = state.Status
        this.running = state.Running.toBoolean()
        this.paused = state.Paused.toBoolean()
        this.restarting = state.Restarting.toBoolean()
        this.dead = state.Dead.toBoolean()
        this.pid = state.Pid.toInteger()
        this.exitCode = state.ExitCode.toInteger()
        this.error = state.Error
        this.startedAt = state.StartedAt
        this.finishedAt = state.FinishedAt
        return this
    }

    boolean isRunning() {
        return running
    }

    boolean isPaused() {
        return paused
    }
}
