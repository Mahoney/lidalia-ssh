package uk.org.lidalia.ssh

import static java.util.concurrent.TimeUnit.HOURS

import static uk.org.lidalia.test.Assert.shouldThrow
import static uk.org.lidalia.ssh.Constants.DEFAULT_TIMEUNIT
import static uk.org.lidalia.ssh.Constants.DEFAULT_TIMEOUT
import static org.junit.Assert.assertSame
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.Mockito.verify
import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq

import org.junit.Test
import org.junit.Before

import uk.org.lidalia.ssh.Command;
import uk.org.lidalia.ssh.CommandResult;
import uk.org.lidalia.ssh.ExitStatus;
import uk.org.lidalia.ssh.PosixUser;
import uk.org.lidalia.ssh.RichConnection;

class RichConnectionTests {

	static final PosixUser ROOT = new PosixUser('root')

    RichConnection richConnection
    MockSSHConnection connectionMock
    Runner runner
	String commandString = 'blah'

    @Before void createConnection() {
        runner = mock(Runner)
        connectionMock = new MockSSHConnection(runner)
        richConnection = new RichConnection(connectionMock, 'url')
    }

    @Test void remoteOpensConnectionIfClosedAndClosesAfterwards() {
        def called = false
        assert !connectionMock.connected
        richConnection.remote {
            assert connectionMock.connected
            called = true
        }
        assert !connectionMock.connected
        assert called
    }

    @Test void remoteDoesNotOpenOrCloseConnectionIfOpen() {
        def called = false
        connectionMock.connected = true
        richConnection.remote {
            assert connectionMock.connected
            called = true
        }
        assert connectionMock.connected
        assert called
    }

    @Test void remoteClosesConnectionOnExceptionIfClosedAtStart() {
        Throwable toThrow = new Throwable()
        assert !connectionMock.connected
        shouldThrow(toThrow) {
            richConnection.remote {
                assert connectionMock.connected
                throw toThrow
            }
        }
        assert !connectionMock.connected
    }

    @Test void remoteLeavesConnectionOpenOnExceptionIfOpenAtStart() {
        connectionMock.connected = true
        Throwable toThrow = new Throwable()
        shouldThrow(toThrow) {
            richConnection.remote {
                assert connectionMock.connected
                throw toThrow
            }
        }
        assert connectionMock.connected
    }

    @Test void runOpensAndClosesConnectionIfClosedAndRunsCommand() {
        def result = mock(CommandResult)
        Command command = new Command(commandString)
        when(runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)).thenReturn(result)

        
        assert !connectionMock.connected
        assertSame result, richConnection.run(command)
        assert !connectionMock.connected
    }

    @Test void runDoesNotOpenOrCloseConnectionIfOpenAndRunsCommand() {
        def result = mock(CommandResult)
        Command command = new Command(commandString)
		when(runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)).thenReturn(result)
        connectionMock.connected = true
		
        assertSame result, richConnection.run(command)
        assert connectionMock.connected
    }

    @Test void runStringOpensAndClosesConnectionIfClosedAndRunsCommand() {
        Command command = new Command(commandString)
        CommandResult result = success('string result')
        when(runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)).thenReturn(result)

        assert !connectionMock.connected
        assert 'string result' == richConnection.run(commandString)
        assert !connectionMock.connected
    }

    @Test void runStringDoesNotOpenOrCloseConnectionIfOpenAndRunsCommand() {
        Command command = new Command(commandString)
        CommandResult result = success('string result')
		when(runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)).thenReturn(result)
        connectionMock.connected = true
		
        assert 'string result' == richConnection.run(commandString)
        assert connectionMock.connected
    }

    @Test void openOpensConnection() {
        assert !connectionMock.connected
        richConnection.open()
        assert connectionMock.connected
    }

    @Test void closeClosesConnection() {
        connectionMock.connected = true
        richConnection.close()
        assert !connectionMock.connected
    }

    @Test void isConnectedReturnsConnectionState() {
        connectionMock.connected = true
        assert richConnection.connected

        connectionMock.connected = false
        assert !richConnection.connected
    }

    @Test void putOpensAndClosesConnectionIfClosedAndRunsCommand() {
        assert !connectionMock.connected
		
        richConnection.put('src', 'target')
		
        assert !connectionMock.connected
		verify(runner).put('src', 'target')
    }

    @Test void putDoesNotOpenOrCloseConnectionIfOpenAndRunsCommand() {
        connectionMock.connected = true
        
        richConnection.put('src', 'target')
		
        assert connectionMock.connected
		verify(runner).put('src', 'target')
    }

    @Test void putAsUserOpensAndClosesConnectionIfClosedAndRunsCommand() {
        when(runner.run(any(Command), eq(DEFAULT_TIMEOUT), eq(DEFAULT_TIMEUNIT))).thenReturn(success())
        assert !connectionMock.connected
		
        richConnection.put('/dir/src', '/dir/target', 'root')
		
        assert !connectionMock.connected
		verify(runner).put('/dir/src', '/tmp')
		verify(runner).run(new Command("cp /tmp/src /dir/target", ROOT), DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)
		verify(runner).run(new Command("rm /tmp/src"), DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)
    }

    @Test void putAsUserDoesNotOpenOrCloseConnectionIfOpenAndRunsCommand() {
		when(runner.run(any(Command), eq(DEFAULT_TIMEOUT), eq(DEFAULT_TIMEUNIT))).thenReturn(success())
        connectionMock.connected = true
		
		richConnection.put('/dir/src', '/dir/target', 'root')
		
        assert connectionMock.connected
		verify(runner).put('/dir/src', '/tmp')
		verify(runner).run(new Command("cp /tmp/src /dir/target", ROOT), DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)
		verify(runner).run(new Command("rm /tmp/src"), DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)
    }

    @Test void runDefaultsToDefaultTimeouts() {
        def result = mock(CommandResult)
        Command command = new Command(commandString)
        when(runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)).thenReturn(result)

        
        assertSame result, richConnection.run(command)
    }

	@Test void runCanTakeDifferentNumberOfDefaultTimeUnit() {
		def result = mock(CommandResult)
		Command command = new Command(commandString)
		when(runner.run(command, 2, DEFAULT_TIMEUNIT)).thenReturn(result)

		assertSame result, richConnection.run(command, 2)
	}

    @Test void runCanTakeADifferentTimeout() {
        def result = mock(CommandResult)
        Command command = new Command(commandString)
		when(runner.run(command, 5, HOURS)).thenReturn(result)

        assertSame result, richConnection.run(command, 5, HOURS)
    }

    @Test void runStringCanTakeAUser() {
        Command command = new Command(commandString, new PosixUser("me"))
        when(runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT)).thenReturn(success('string result'))

        assert !connectionMock.connected
        assert 'string result' == richConnection.run(commandString, 'me')
        assert !connectionMock.connected
    }

    @Test void runStringCanTakeAUserAndADifferentNumberOfDefaultTimeUnit() {
        Command command = new Command(commandString, new PosixUser("me"))
		when(runner.run(command, 20, DEFAULT_TIMEUNIT)).thenReturn(success('string result'))

        assert !connectionMock.connected
        assert 'string result' == richConnection.run(commandString, 'me', 20)
        assert !connectionMock.connected
    }

    @Test void runStringCanTakeAUserAndADifferentTimeout() {
        Command command = new Command(commandString, new PosixUser("me"))
		when(runner.run(command, 5, HOURS)).thenReturn(success('string result'))

        assert !connectionMock.connected
        assert 'string result' == richConnection.run(commandString, 'me', 5, HOURS)
        assert !connectionMock.connected
    }
	
	private CommandResult success(String stdout = '') {
		return new CommandResult(null, new ExitStatus(0), stdout, '', 1L)
	}
}
