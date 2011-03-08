package uk.org.lidalia.ssh

import static java.util.concurrent.TimeUnit.HOURS

import static uk.org.lidalia.ssh.Assert.shouldThrow
import static uk.org.lidalia.ssh.Connection.DEFAULT_TIMEUNIT
import static uk.org.lidalia.ssh.Connection.DEFAULT_TIMEOUT
import static org.junit.Assert.assertSame

import org.junit.Test
import org.junit.Before
import org.gmock.WithGMock

import uk.org.lidalia.ssh.Command;
import uk.org.lidalia.ssh.CommandResult;
import uk.org.lidalia.ssh.ExitStatus;
import uk.org.lidalia.ssh.PosixUser;
import uk.org.lidalia.ssh.RichConnection;

@WithGMock
class RichConnectionTests {

    RichConnection richConnection
    MockSSHConnection connectionMock
    def runner
	String commandString = 'blah'

    @Before void createConnection() {
        runner = mock()
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
        runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(result)

        play {
            assert !connectionMock.connected
            assertSame result, richConnection.run(command)
            assert !connectionMock.connected
        }
    }

    @Test void runDoesNotOpenOrCloseConnectionIfOpenAndRunsCommand() {
        def result = mock(CommandResult)
        Command command = new Command(commandString)
        runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(result)

        connectionMock.connected = true
        play {
            assertSame result, richConnection.run(command)
            assert connectionMock.connected
        }
    }

    @Test void runStringOpensAndClosesConnectionIfClosedAndRunsCommand() {
        Command command = new Command(commandString)
        CommandResult result = success('string result')
        runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(result)

        play {
            assert !connectionMock.connected
            assert 'string result' == richConnection.run(commandString)
            assert !connectionMock.connected
        }
    }

    @Test void runStringDoesNotOpenOrCloseConnectionIfOpenAndRunsCommand() {
        Command command = new Command(commandString)
        CommandResult result = success('string result')
        runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(result)

        connectionMock.connected = true
        play {
            assert 'string result' == richConnection.run(commandString)
            assert connectionMock.connected
        }
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
        runner.put('src', 'target')

        play {
            assert !connectionMock.connected
            richConnection.put('src', 'target')
            assert !connectionMock.connected
        }
    }

    @Test void putDoesNotOpenOrCloseConnectionIfOpenAndRunsCommand() {
        runner.put('src', 'target')

        connectionMock.connected = true
        play {
            richConnection.put('src', 'target')
            assert connectionMock.connected
        }
    }

    @Test void putAsUserOpensAndClosesConnectionIfClosedAndRunsCommand() {
        runner.put('/dir/src', '/tmp')
        def copy = new Command("cp /tmp/src /dir/target", new PosixUser('root'))
        runner.run(copy, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(success())
        def delete = new Command("rm /tmp/src")
        runner.run(delete, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(success())

        play {
            assert !connectionMock.connected
            richConnection.put('/dir/src', '/dir/target', 'root')
            assert !connectionMock.connected
        }
    }

    @Test void putAsUserDoesNotOpenOrCloseConnectionIfOpenAndRunsCommand() {
        runner.put('/dir/src', '/tmp')
        def copy = new Command("cp /tmp/src /dir/target", new PosixUser('root'))
        runner.run(copy, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(success())
        def delete = new Command("rm /tmp/src")
        runner.run(delete, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(success())

        connectionMock.connected = true
        play {
            richConnection.put('/dir/src', '/dir/target', 'root')
            assert connectionMock.connected
        }
    }

    @Test void runDefaultsToDefaultTimeouts() {
        def result = mock(CommandResult)
        Command command = new Command(commandString)
        runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(result)

        play {
            assertSame result, richConnection.run(command)
        }
    }

	@Test void runCanTakeDifferentNumberOfDefaultTimeUnit() {
		def result = mock(CommandResult)
		Command command = new Command(commandString)
		runner.run(command, 2, DEFAULT_TIMEUNIT).returns(result)

		play {
			assertSame result, richConnection.run(command, 2)
		}
	}

    @Test void runCanTakeADifferentTimeout() {
        def result = mock(CommandResult)
        Command command = new Command(commandString)
        runner.run(command, 5, HOURS).returns(result)

        play {
            assertSame result, richConnection.run(command, 5, HOURS)
        }
    }

    @Test void runStringCanTakeAUser() {
        Command command = new Command(commandString, new PosixUser("me"))
        runner.run(command, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT).returns(success('string result'))

        play {
            assert !connectionMock.connected
            assert 'string result' == richConnection.run(commandString, 'me')
            assert !connectionMock.connected
        }
    }

    @Test void runStringCanTakeAUserAndADifferentNumberOfDefaultTimeUnit() {
        Command command = new Command(commandString, new PosixUser("me"))
        runner.run(command, 20, DEFAULT_TIMEUNIT).returns(success('string result'))

        play {
            assert !connectionMock.connected
            assert 'string result' == richConnection.run(commandString, 'me', 20)
            assert !connectionMock.connected
        }
    }

    @Test void runStringCanTakeAUserAndADifferentTimeout() {
        Command command = new Command(commandString, new PosixUser("me"))
        runner.run(command, 5, HOURS).returns(success('string result'))

        play {
            assert !connectionMock.connected
            assert 'string result' == richConnection.run(commandString, 'me', 5, HOURS)
            assert !connectionMock.connected
        }
    }
	
	private CommandResult success(String stdout = '') {
		return new CommandResult(null, new ExitStatus(0), stdout, '', 1L)
	}
}
