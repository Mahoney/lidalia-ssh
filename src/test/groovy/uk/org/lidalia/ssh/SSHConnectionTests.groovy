package uk.org.lidalia.ssh

import static uk.org.lidalia.test.Assert.shouldThrow
import static org.junit.Assert.assertSame
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.Mockito.inOrder
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.atLeastOnce

import org.junit.After;
import org.junit.Test
import org.junit.runner.RunWith

import uk.org.lidalia.ssh.Command
import uk.org.lidalia.ssh.CommandFailedException
import uk.org.lidalia.ssh.CommandResult
import uk.org.lidalia.ssh.ExitStatus
import uk.org.lidalia.ssh.CommandInterruptedException
import uk.org.lidalia.ssh.SSHConnection
import uk.org.lidalia.ssh.CommandTimeoutException

import ch.ethz.ssh2.Connection
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse
import ch.ethz.ssh2.SCPClient
import ch.ethz.ssh2.Session
import java.util.concurrent.TimeUnit

class SSHConnectionTests {

	Connection connMock = mock(Connection)
    Session sessionMock = mock(Session)
    SSHConnection sshConnection = new SSHConnection('url', 'user', 'password')
	SCPClient scpClientMock = mock(SCPClient)
	
	@After
	void resetConstructors() {
		Connection.metaClass = null
		SCPClient.metaClass = null
	}
	
	private void expectConnectionToBeOpened() {
		Connection.metaClass.constructor = { String host ->
			return connMock
		}
		when(connMock.authenticateWithPassword('user', 'password')).thenReturn(true)
	}
	
	@Test
	void connectConnects() {
		expectConnectionToBeOpened()

		sshConnection.open()
		
		def inOrder = inOrder(connMock)
		inOrder.verify(connMock).connect()
		inOrder.verify(connMock).authenticateWithPassword('user', 'password')
		assert sshConnection.connected
	}
	
	@Test
	void connectWithInvalidPassword() {
		sshConnection = new SSHConnection('url', 'user', 'wrongpass')
		expectConnectionToBeOpened()

		def exception = shouldThrow(IOException) {
			sshConnection.open()
		}
		
		def inOrder = inOrder(connMock)
		inOrder.verify(connMock).connect()
		inOrder.verify(connMock).close()
		assert !sshConnection.connected
		assert "Authentication failed for user on url" == exception.message
	}

    @Test
    void closeWorksOnClosedConnection() {
        sshConnection.close()
		
		assert !sshConnection.connected
    }

    @Test
    void closeWorksOnOpenConnection() {
        expectConnectionToBeOpened()

        sshConnection.open()
        sshConnection.close()

		verify(connMock).close()
        assert !sshConnection.connected
    }

    @Test
    void putCallsScpClient() {
        expectConnectionToBeOpened()
		SCPClient.metaClass.constructor = { Connection conn ->
			assert conn == connMock
			return scpClientMock
		}

        sshConnection.open()
        sshConnection.put("src", "target")

		verify(scpClientMock).put("src", "target", "0777")
    }

    @Test void runReturnsCommandResult() {
        expectConnectionToBeOpened()
		when(connMock.openSession()).thenReturn(sessionMock)
        expectCommandToReturn('grep "blah"', 'stdout', 'stderr', 0)

        sshConnection.open()
        def command = new Command('grep "blah"')
        def result = sshConnection.run(command)
		
        assert command.is(result.command)
        assert 'stdout' == result.out
        assert 'stderr' == result.err
        assert new ExitStatus(0) == result.exitStatus
		verify(sessionMock).execCommand('grep "blah"')
		verify(sessionMock).close()
    }

    @Test void runThrowsCommandFailedException() {
        expectConnectionToBeOpened()
		when(connMock.openSession()).thenReturn(sessionMock)
        expectCommandToReturn('grep "blah"', 'stdout', 'stderr', 5)

        sshConnection.open()
        def command = new Command('grep "blah"')
        def commandFailedException = shouldThrow(CommandFailedException) {
            sshConnection.run(command)
        }
        def result = commandFailedException.result
		
		assert result.command.is(command)
        assert 'stdout' == result.out
        assert 'stderr' == result.err
        assert new ExitStatus(5) == result.exitStatus
		verify(sessionMock).execCommand('grep "blah"')
		verify(sessionMock).close()
    }

    @Test void runThrowsTimedoutException() {
        expectConnectionToBeOpened()
		when(connMock.openSession()).thenReturn(sessionMock)
        expectCommandToReturn('grep "blah"', 'stdout', 'stderr', null)

        sshConnection.open()
        def command = new Command('grep "blah"')
        def timedoutCommandException = shouldThrow(CommandTimeoutException) {
            sshConnection.run(command, 1, TimeUnit.MILLISECONDS)
        }
        def result = timedoutCommandException.result
		
		assert result.command.is(command)
        assert 'stdout' == result.out
        assert 'stderr' == result.err
        assert result.exitStatus == null
        assert timedoutCommandException.timeout == 1
        assert timedoutCommandException.timeUnit == TimeUnit.MILLISECONDS
		verify(sessionMock).execCommand('grep "blah"')
		verify(sessionMock, atLeastOnce()).close()
    }

    @Test void runThrowsInterruptedException() {
        expectConnectionToBeOpened()
		when(connMock.openSession()).thenReturn(sessionMock)
        expectCommandToReturn('grep "blah"', 'stdout', 'stderr', null)

        def command = new Command('grep "blah"')
        CommandInterruptedException interruptedException = null
        def thread = Thread.start {
            sshConnection.open()
            interruptedException = shouldThrow(CommandInterruptedException) {
                sshConnection.run(command)
            }
        }
        Thread.sleep(50L)
        thread.interrupt()
        Thread.sleep(50L)

        def result = interruptedException.result
		
		assert result.command.is(command)
        assert 'stdout' == result.out
        assert 'stderr' == result.err
        assert result.exitStatus == null
		verify(sessionMock).execCommand('grep "blah"')
		verify(sessionMock, atLeastOnce()).close()
    }

    private void expectCommandToReturn(String commandString, String stdout, String stderr, Integer status) {
        when(sessionMock.stdout).thenReturn(new ByteArrayInputStream(stdout.bytes))
        when(sessionMock.stderr).thenReturn(new ByteArrayInputStream(stderr.bytes))
        when(sessionMock.exitStatus).thenReturn(status)
    }
}
