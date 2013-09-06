zanata-sync-script
==================

Configurable script to do simple synchronization between a Zanata instance and a code repository.

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

Configuration File
------------------

The configuration file (specified via the `--config` command line argument), is a json file with the following structure:

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
