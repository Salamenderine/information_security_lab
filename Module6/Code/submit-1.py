import os 
import subprocess
import time 

def terminate():
    os.system("pkill -9 node")
    os.system('pkill -9 gdb')
    
def start_manager_and_peripheral():
    os.system("bash /home/isl/t1/run_manager.sh")
    os.system("bash /home/isl/t1/run_peripheral.sh")
    
def gdb_cmd(gdb, cmd):
    cmd = cmd + "\n"
    cmd = cmd.encode()
    gdb.stdin.write(cmd)
    gdb.stdin.flush()

def inject_message(msg):
    gdb = subprocess.Popen(['gdb', '--args', 'python3', '/home/isl/t1/sp_server.py'], stdin=subprocess.PIPE)  
    gdb_cmd(gdb, "add-symbol-file /home/isl/.local/lib/stringparser_core.so")
    gdb_cmd(gdb, 'b gcm_crypt_and_tag')
    gdb_cmd(gdb, 'r')

    os.system("node --no-warnings /home/isl/t1/remote_party  | tee /home/isl/t1/remote_party.log &")

    gdb_cmd(gdb, 'break gcm_crypt_and_tag')
    gdb_cmd(gdb, 'c')
    gdb_cmd(gdb, msg)
    gdb_cmd(gdb, 'c 100')
    time.sleep(2)

    gdb.terminate()
    os.system("pkill -9 gdb")


def main():
    cmd1 = 'set {char[40]} input = "<mes><action type=\\"key-update\\"/></mes>"'
    cmd2 = 'set {char[20]} input = "redeemToken, token"'
    
    terminate()
    start_manager_and_peripheral()
    inject_message(cmd1)
    inject_message(cmd2)
    terminate()

if __name__ == "__main__":
    main()