## Access

You will get your personal ip address pushed to the root of your repo in the file:
`cloud_ip.txt`

The TAs will then push the identity (ssh-public-key) you provided onto the VMs giving you access as the `vagrant` user. You will be able to use a similar command to log in as described in the first labsheet, without needing to change the port but needing to replace the localhost address with your cloud IP address.

In order to access the grafana dashboard, you must also instruct ssh to tunnel some ports (this is already configured in the local virtualbox VMs and does not need to be done manually).

We are telling the remote machine to forward the Grafana port 3000 to port 8011 locally, such that you can access the grafana dashboard through the browser, as is already configured for local VMs. So the command becomes:

```
ssh -L 8011:localhost:3000 vagrant@<my_cloud_ip_address>
```

[Check out this guide for more information about ssh tunneling.](https://www.ssh.com/academy/ssh/tunneling)

## Pushing code to the cloud VM

Obviously, you will not be able to set up a shared folder in the same way as when you are running a local instance of the VM in VirtualBox. 

But the vm still expects to find the configuration files (especially IPs.yml) and source to be in `/home/vagrant/src`, as it would be in a locally run image. You will notice that this folder is not present. 
To achieve this you can simply clone your git repository directly from inside the VM with the following command already listed in the labsheet:

```
git clone <your_isl_netsec_repo_URL> /home/vagrant/src
```

This will result in the expected directory structure. And you can now run:
```
source configure_isl
```

You can either develop directly in the cloud VM if you are comfortable with command line editors, or you can still develop on your local machine, but will need to go through an extra step of committing and pushing the code locally, and pulling the code on the cloud VM before you can run tests. 

Alternatively, you can use VSCode's "Remote - SSH" plugin which allows you to develop on the VM while still benefiting from the familiar VSCode user interface. This approach offers convenient browsing of the VM's filesystem and you can open multiple terminals without the need of a terminal multiplexer (e.g., `tmux`). The added functionality comes at the cost of higher CPU load on your VM, which may, given the ressource constraints of your VM, lead to degraded performance. We uploaded a brief video outlining the installation and setup process on moodle [here](https://polybox.ethz.ch/index.php/s/2KZUtvsGoi5wubi).

You are free to use any way of developing and deploying, as long as you hand in your final solution via Gitlab **before** the deadline.