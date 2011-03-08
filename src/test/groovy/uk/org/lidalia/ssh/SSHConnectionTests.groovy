package uk.org.lidalia.ssh

import static uk.org.lidalia.ssh.Assert.shouldThrow
import static org.junit.Assert.assertSame

import org.junit.Test
import org.gmock.WithGMock

import uk.org.lidalia.ssh.Command;
import uk.org.lidalia.ssh.CommandFailedException;
import uk.org.lidalia.ssh.CommandResult;
import uk.org.lidalia.ssh.ExitStatus;
import uk.org.lidalia.ssh.CommandInterruptedException;
import uk.org.lidalia.ssh.SSHConnection;
import uk.org.lidalia.ssh.CommandTimeoutException;

import ch.ethz.ssh2.Connection
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse
import ch.ethz.ssh2.SCPClient
import ch.ethz.ssh2.Session
import java.util.concurrent.TimeUnit

@WithGMock
class SSHConnectionTests {


    Connection connMock
    Session sessionMock
    SSHConnection sshConnection = new SSHConnection('url', 'user', 'password')

    @Test
    void connectConnects() {
        expectConnectionToBeOpened()

        play {
            sshConnection.open()
            assertTrue(sshConnection.connected)
        }
    }

    private void expectConnectionToBeOpened() {
        connMock = mock(Connection, constructor('url'))
        connMock.connect()
        connMock.authenticateWithPassword('user', 'password').returns(true)
        sessionMock = mock(Session)
        connMock.openSession().returns(sessionMock).atMostOnce()
    }

    @Test
    void closeWorksOnClosedConnection() {
        sshConnection.close()
        assertFalse(sshConnection.connected)
    }

    @Test
    void closeWorksOnOpenConnection() {
        expectConnectionToBeOpened()
        connMock.close()

        play {
            sshConnection.open()
            sshConnection.close()
            assertFalse(sshConnection.connected)
        }
    }

    @Test
    void putCallsScpClient() {
        expectConnectionToBeOpened()

        SCPClient scpClientMock = mock(SCPClient, constructor(connMock))
        scpClientMock.put("src", "target", "0777")

        play {
            sshConnection.open()
            sshConnection.put("src", "target")
        }

    }

    @Test void runReturnsCommandResult() {
        expectConnectionToBeOpened()
        expectCommandToReturn('grep "blah"', 'stdout', 'stderr', 0)
        sessionMock.close()

        play {
            sshConnection.open()
            Command command = new Command('grep "blah"')
            CommandResult result = sshConnection.run(command)
            assertSame command, result.command
            assert 'stdout' == result.out
            assert 'stderr' == result.err
            assert new ExitStatus(0) == result.exitStatus
        }
    }

    @Test void runThrowsCommandFailedException() {
        expectConnectionToBeOpened()
        expectCommandToReturn('grep "blah"', 'stdout', 'stderr', 5)
        sessionMock.close().atLeastOnce()

        play {
            sshConnection.open()
            Command command = new Command('grep "blah"')
            CommandFailedException commandFailedException = shouldThrow(CommandFailedException) {
                sshConnection.run(command)
            }
            CommandResult result = commandFailedException.result
            assertSame command, result.command
            assert 'stdout' == result.out
            assert 'stderr' == result.err
            assert new ExitStatus(5) == result.exitStatus
        }
    }

    @Test void runThrowsTimedoutException() {
        expectConnectionToBeOpened()
        expectCommandToReturn('grep "blah"', 'stdout', 'stderr', null)
        sessionMock.close().atLeastOnce()

        play {
            sshConnection.open()
            Command command = new Command('grep "blah"')
            CommandTimeoutException timedoutCommandException = shouldThrow(CommandTimeoutException) {
                sshConnection.run(command, 1, TimeUnit.MILLISECONDS)
            }
            CommandResult result = timedoutCommandException.result
            assertSame command, result.command
            assert 'stdout' == result.out
            assert 'stderr' == result.err
            assert result.exitStatus == null
            assert timedoutCommandException.timeout == 1
            assert timedoutCommandException.timeUnit == TimeUnit.MILLISECONDS
        }
    }

    @Test void runThrowsInterruptedException() {
        expectConnectionToBeOpened()
        expectCommandToReturn('grep "blah"', 'stdout', 'stderr', null)
        sessionMock.close().atLeastOnce()

        Command command = new Command('grep "blah"')
        CommandInterruptedException interruptedException = null
        def thread = Thread.start {
            play {
                sshConnection.open()
                interruptedException = shouldThrow(CommandInterruptedException) {
                    sshConnection.run(command)
                }
            }
        }
        Thread.sleep(50L)
        thread.interrupt()
        Thread.sleep(50L)

        CommandResult result = interruptedException.result
        assertSame command, result.command
        assert 'stdout' == result.out
        assert 'stderr' == result.err
        assert result.exitStatus == null
    }

    private void expectCommandToReturn(String commandString, String stdout, String stderr, Integer status) {
        sessionMock.execCommand(commandString)
        sessionMock.stdout.returns(new ByteArrayInputStream(stdout.bytes))
        sessionMock.stderr.returns(new ByteArrayInputStream(stderr.bytes))
        sessionMock.exitStatus.returns(status).stub()
    }
}
