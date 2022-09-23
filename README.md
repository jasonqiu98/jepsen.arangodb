# jepsen.arangodb

A Clojure library designed to test ArangoDB.

## Set up your environment for this project

Here I use an example to set up the environment on Linux Mint 21 / Ubuntu 22.04.

1. Install [`virtualbox`](https://www.virtualbox.org/wiki/Linux_Downloads) and [`vagrant`](https://www.vagrantup.com/downloads) on your local machine.
2. Set up your local machine as the control node of this Jepsen test.
    - Install [`Leiningen`](https://leiningen.org/) with `sudo apt install leiningen`
    - Install other dependencies using `sudo apt install libjna-java gnuplot graphviz`
3. Create a public/private key pair for `vagrant` VMs
    1. Generate by `ssh-keygen`. Enter the path you want to save the key. Here I use the default path `~/.ssh/id_rsa`.
    2. Enter passphrase. Here I use the default value (empty for no passphrase). After that, enter same passphrase again.
    3. Then check the results. Here I have the following results.
    ```
    Your identification has been saved in ~/.ssh/id_rsa
    Your public key has been saved in ~/.ssh/id_rsa.pub
    The key fingerprint is:
    SHA256:vKQ4S16rSqkS7zCFShzXMNNVlog/8iRuPT7OWA+TjQI jasonqiu98@jasonqiu98-Legion-5-Pro-16ACH6H
    ```
    4. Copy, paste and overwrite the keys under the root folder of this project. Remember to replace the key path to your own path.
    ```
    cp ~/.ssh/id_rsa ./vagrant/vagrant_ssh_key
    cp ~/.ssh/id_rsa.pub ./vagrant/shared/vagrant_ssh_key.pub
    ```
4. Initialize Vagrant VMs
    1. Enter the `vagrant` folder by `cd vagrant`.
    2. Run `/bin/bash ./init/init.sh` in the project folder. This script destroys all the vagrant VMs and resets/starts new VMs. 
    3. Run `/bin/bash ./init/follow-up.sh` in the project folder. This script searches public keys of the VMs and writes to `known_hosts`.
        - Note! This script may fail as the VMs need time to get ready for further operations. You may need to wait a while before running this script.
        - A failure example (as you may see this first)
            - ```
              192.168.56.101 error
              192.168.56.102 error
              192.168.56.103 error
              192.168.56.104 error
              192.168.56.105 error
              ```
        - A success example
            - ```
              # 192.168.56.101:22 SSH-2.0-OpenSSH_8.4p1 Debian-5+deb11u1
              192.168.56.101 ok
              # 192.168.56.102:22 SSH-2.0-OpenSSH_8.4p1 Debian-5+deb11u1
              192.168.56.102 ok
              # 192.168.56.103:22 SSH-2.0-OpenSSH_8.4p1 Debian-5+deb11u1
              192.168.56.103 ok
              # 192.168.56.104:22 SSH-2.0-OpenSSH_8.4p1 Debian-5+deb11u1
              192.168.56.104 ok
              # 192.168.56.105:22 SSH-2.0-OpenSSH_8.4p1 Debian-5+deb11u1
              192.168.56.105 ok
              ```
    4. Return to the parent folder by `cd ..`.

Now the VM is started and waits for the following commands.

## Start the test

- Run `/bin/bash ./run.sh` to start your test! This script will restart all VMs before running the test by default.
- If you just followed the previous section, you may want to skip the restart process by using `/bin/bash ./run.sh --skip-vagrant` instead.
- Try `/bin/bash ./run.sh --time-limit 20 -r 1` (or `/bin/bash ./run.sh --skip-vagrant --time-limit 20 -r 1`) to set a time limit of 20 seconds.

Errors found? Consider generate a new public/private key pair by `ssh-keygen` and goes along the process again. Check you `ssh-agent` as well.

## Acknowledgement

This project is inspired by [jepsen.rqlite](https://github.com/wildarch/jepsen.rqlite). The setup follows a similar procedure.
