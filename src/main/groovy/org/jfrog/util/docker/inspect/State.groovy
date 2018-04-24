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

    State init(Map inspect, boolean isExec = false) {
        Map map = isExec ? inspect : inspect.get(STATE)
        this.status = map.Status
        this.running = map.Running?.toBoolean()
        this.paused = map.Paused?.toBoolean()
        this.restarting = map.Restarting?.toBoolean()
        this.dead = map.Dead?.toBoolean()
        this.pid = map.Pid?.toInteger()
        this.exitCode = map.ExitCode?.toInteger()
        this.error = map.Error
        this.startedAt = map.StartedAt
        this.finishedAt = map.FinishedAt
        return this
    }

    boolean isRunning() {
        return running
    }

    boolean isPaused() {
        return paused
    }
}
