package uk.org.lidalia.ssh

import static java.util.concurrent.TimeUnit.MINUTES
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

import org.junit.Test

import uk.org.lidalia.ssh.Command;
import uk.org.lidalia.ssh.CommandResult;
import uk.org.lidalia.ssh.Connection;
import uk.org.lidalia.ssh.PosixUser;
import static org.junit.Assert.assertSame

class CommandTests {

    @Test void commandIsSameIfNoUser() {
        def command = new Command('grep "blah"')
        assert 'grep "blah"' == command.command
    }

    @Test void commandIsPipedToSudoIfUser() {
        def command = new Command('grep "blah"', new PosixUser('ubuntu'))
        assert 'echo "grep \\"blah\\"" | sudo -s -u ubuntu' == command.command
    }

    @Test void commandToStringIsCommand() {
        def command = new Command('grep "blah"', new PosixUser('ubuntu'))
        assert command.toString() == command.command
    }

    @Test void commandEqualsCommand() {
        assert new Command('blah')                          == new Command('blah')
        assert new Command('blah', null)                    == new Command('blah', null)
        assert new Command('blah', new PosixUser('ubuntu')) == new Command('blah', new PosixUser('ubuntu'))

        assert new Command('blah')                          != new Command('foo')
        assert new Command('blah', new PosixUser('ubuntu')) != new Command('blah')
		assert new Command('blah', new PosixUser('ubuntu')) != new Command('blah', null)
        assert new Command('blah', new PosixUser('ubuntu')) != new Command('blah', new PosixUser('other'))
        assert new Command('blah', new PosixUser('ubuntu')) != new Command('foo', new PosixUser('ubuntu'))
    }

    @Test void runDelegatesToConnectionRun() {

        def connection = mock(Connection)
        def expectedResult = mock(CommandResult)
        def command = new Command('grep "blah"')
        when(connection.run(command, 10, MINUTES)).thenReturn(expectedResult)

        def actualResult = command.run(connection, 10, MINUTES)
        assert actualResult == expectedResult
    }
}
