zanata-sync-script
==================

Configurable script to do simple synchronization between a Zanata instance and a code repository.

NOTE: This script is NOT currently suitable for doing an initial Git push from Zanata. This means that there should
already be an existing Git repository with translatable files set up. Doing a first push from Git to Zanata is 
currently supported however.

Pre-requisites
--------------

Groovy (version 1.8+) must be installed and available to run the script.
Java (version 1.6+) must be installed.

Usage
-----

To synchronize Zanata translations into your git code repository.

```sh
groovy ZanataSync zanata-to-git --config config.json
```

To synchronize source files in a Git code repository with Zanata.

```sh
groovy ZanataSync git-to-zanata --config config.json
```

The script will create a workspace directory where the repository's content will be contained. All commands will be executed from
this directory (git, zanata client, etc.). The `--config` option is relative to this workspace directory. Basically, it's relative
to your source repository's location. This also means that if the `zanata.xml` is kept on your source repository, then it may be
used directly by the sync script when connecting to Zanata.

Configuration File
------------------

The configuration file (specified via the `--config` command line argument), is a json file with the following structure:

```json
{
    "zanata": {
        "username": "Zanata instance's user name",
        "key": "Zanata instance's api key",
        "options": "General options that will apply whenever invoking the zanata client (any command)",
        "options_push": "Options that will be applied when the zanata client's push command is invoked",
        "options_pull": "Options that will be applied when the zanata client's pull command is invoked",
        "pre_push": "(Optional) Command to run before pushing to Zanata. (i.e. to generate sources for instance.)"
    },
    "vc": {
        "type": "git (Currently only git is supported, more may come in the future)",
        "origin": {
            "repo": "Git repo url where source files will be pulled from",
            "branch": "Git branch where source files will be pulled from"
        },
        "target": {
            "repo": "Git repo url where translated files will be pushed",
            "branch": "Git branch where translated files will be pushed",
            "force_push": "(true | false) Whether to force push. Useful when you always want the latest translations pushed."
        }
    },
    "email": {
        "from":"From email address to use.",
        "tolist":"Comma-separated recipients list for notification emails sent from the script",
        "host":"Email Server host",
        "port":"Email Server port (Defaults to 25 if not provided)",
        "user":"Email server username (If the server is not authenticated, remove this line)",
        "password":"Email server password (If the server is not authenticated, remove this line)",
        "ssl":"(true | false) Whether the server uses ssl",
        "enableStartTLS":"(true | false) Whether to use the start TLS command."
    }
}
```

If the "origin" and "target" repositories and branches are the same, then the target configuration may be specified like this:

```json
        "target": "origin"
```

If the "email" section is not provided, the script will not send emails.

The following is a sample configuration file:

```json
{
    "zanata": {
        "username": "zanatauser",
        "key": "abcdef12345",
        "options": "--src-dir . --trans-dir .",
        "options_push": "--includes **//*messages.properties",
        "options_pull": ""
    },
    "vc": {
        "type": "git",
        "origin": {
            "repo": "git://mygitserver.com/mygit.git",
            "branch": "master"
        },
        "target": {
            "repo": "git+ssh://mygitserver.com/mygit.git",
            "branch": "target_branch",
            "force_push": true
        }
    }
}
```

   
If no configuration file is specified, the script will look for a file called `zanata-sync.json` in the current directory.
