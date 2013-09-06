import groovy.grape.Grape
import groovy.json.JsonSlurper
import org.codehaus.groovy.tools.RootLoader
import org.kohsuke.args4j.Argument
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import org.kohsuke.args4j.spi.SubCommand
import org.kohsuke.args4j.spi.SubCommandHandler
import org.kohsuke.args4j.spi.SubCommands
import org.zanata.client.ZanataClient

@Grab(group = "org.zanata", module = "zanata-cli", version = "3.1.0")

/**
 * Performs synchronization activities between a Zanata server and its code repository.
 *
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
def scriptCmd = new ScriptCmd()
new CmdLineParser(scriptCmd).parseArgument(args)
scriptCmd.command.run()

// Script's auxiliary classes ==========================================================================================

class ScriptCmd {
    @Argument(handler = SubCommandHandler.class)
    @SubCommands([
    @SubCommand(name = "git-to-zanata", impl = GitToZanataCmd.class),
    @SubCommand(name = "zanata-to-git", impl = ZanataToGitCmd.class)
    ])
    Command command
}

abstract class Command {
    @Option(name = "--config", usage = "Sync configuration file (JSON format)")
    File configFile = new File("zanata-sync.json")

    final String workingDir = "./working_git"

    def getConfig = {
        def slurper = new JsonSlurper()
        slurper.parse(configFile.newReader())
    }

    def runCommand = { def cmd, String dir = ".", boolean failIfUnsuccessful = false ->
        println()
        println cmd
        println()

        def proc = cmd.execute([], new File(dir))
        proc.consumeProcessOutput(System.out, System.err)
        int exitValue = proc.waitFor()

        if (failIfUnsuccessful && exitValue != 0) {
            throw new RuntimeException("Failed to run command")
        }
    }

    abstract void run();
}

class GitToZanataCmd extends Command {
    @Override
    void run() {
        // Pull the latest from Git
        new GitPullCmd().run()
        // Push to Zanata
        new ZanataPushCmd().run()
    }
}

class ZanataPushCmd extends Command {
    @Override
    void run() {
        def config = getConfig()

        // Configured Parameters
        String userName = config.zanata.username
        String apiKey = config.zanata.key
        String extraOpts = "${config.zanata.options ?: ''} ${config.zanata.options_push ?: ''}"

        def root = new RootLoader(this.class.classLoader)
        Grape.grab([group: "org.zanata", module: "zanata-cli", version: "3.1.0", classLoader: root])
        def cp = root.URLs.collect { it.file }.join(File.pathSeparator)

        def ant = new AntBuilder()
        def cmd = ["-B", "push", "--username", userName, "--key", apiKey]
        ant.java(fork: true, classname: ZanataClient.class.name, classpath: cp, dir: workingDir) {
            cmd.addAll(extraOpts.split(" "))
            cmd.each { arg(value: it) }
        }
    }
}


class ZanataPullCmd extends Command {
    @Override
    void run() {
        def config = getConfig()

        // Configured Parameters
        String userName = config.zanata.username
        String apiKey = config.zanata.key
        String extraOpts = "${config.zanata.options ?: ''} ${config.zanata.options_pull ?: ''}"

        def root = new RootLoader(this.class.classLoader)
        Grape.grab([group: "org.zanata", module: "zanata-cli", version: "3.1.0", classLoader: root])
        def cp = root.URLs.collect { it.file }.join(File.pathSeparator)

        def ant = new AntBuilder()
        def cmd = ["-B", "pull", "--username", userName, "--key", apiKey]
        ant.java(fork: true, classname: ZanataClient.class.name, classpath: cp, dir: workingDir) {
            cmd.addAll(extraOpts.split(" "))
            cmd.each { arg(value: it) }
        }
    }
}

class ZanataToGitCmd extends Command {
    @Override
    void run() {
        def config = getConfig()

        String gitRepoLoc = config.vc.target == "origin" ? config.vc.origin.repo : config.vc.target.repo
        String originBranch = config.vc.origin.branch
        String targetBranch = config.vc.target.branch
        boolean forcePush = config.vc.target.force_push ?: false
        final String commitComment = "Zanata Sync update from Zanata."

        // Pull the latest from Git
        new GitPullCmd().run()

        // Push sources to Zanata
        new ZanataPushCmd().run()

        // Commit and push to Git
        new File(workingDir).mkdirs()

        // Checkout the origin branch
        runCommand "git fetch", workingDir
        runCommand "git checkout origin/${originBranch}", workingDir, true

        // Pull translations from Zanata
        ZanataPullCmd zanataPull = new ZanataPullCmd()
        zanataPull.run()

        // Create the new branch and check it out
        runCommand "git branch -f ${targetBranch}", workingDir
        runCommand "git checkout ${targetBranch}", workingDir

        // Git create a commit (Adds everything that has changed)
        runCommand "git add .", workingDir
        runCommand "git rm -f -r --ignore-unmatch .zanata-cache", workingDir
        runCommand(["git", "commit", "-m", commitComment], workingDir, true)

        // Git push
        runCommand "git push ${forcePush ? "-f" : ""} ${gitRepoLoc} ${targetBranch}", workingDir, true
    }
}

class GitPullCmd extends Command {
    @Override
    void run() {
        def config = getConfig()
        String gitRepoLoc = config.vc.origin.repo
        String originBranch = config.vc.origin.branch

        new File(workingDir).mkdirs()

        // GIT Clone
        // TODO determine if git repo needs to be cloned or updated (right now it's not failing when re-cloning)
        runCommand "git clone ${gitRepoLoc} ${workingDir}"

        // Checkout the right branch
        runCommand "git checkout ${originBranch}", workingDir, true

        // GIT Pull
        runCommand "git pull", workingDir, true
    }
}