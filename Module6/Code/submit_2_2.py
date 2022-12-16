import sys
import os

CORR_ADDR = "0x401d83"
WRONG_ADDR = "0x401d97"
SGXPath = "/home/isl/pin-3.11-97998-g7ecce2dac-gcc-linux-master/source/tools/SGXTrace"
ASCII_range = list(range(ord('a'), ord('z') + 1))
Alphabets = [chr(i) for i in ASCII_range]

def attemp_pwd(char, corr_char):
    pwd = char * 31
    trace_path = '/home/isl/t2_2/traces/{}'.format(pwd)
    if not os.path.exists('/home/isl/t2_2/traces'):
        os.mkdir('/home/isl/t2_2/traces')
    run_tool_cmd = "../../../pin -t ./obj-intel64/SGXTrace.so -o {} -trace 1 -- ~/t2_2/password_checker_2 {}".format(trace_path, pwd)
    os.system('cd {}; {}'.format(SGXPath, run_tool_cmd))

    addrs = []
    with open(trace_path, 'r') as file:
        for line in file:
            if "E:" in line and ":C" in line:
                addrs.append(line.split(":")[1])

    idx = 0
    for addr in addrs:
        if WRONG_ADDR == addr:
            idx += 1    
        elif CORR_ADDR == addr:
            corr_char[idx] = char
            

def main():
    # Sanity check
    assert len(sys.argv) == 2, "Require only one argument: trace_id"

    id = sys.argv[1]

    corr_char = {}
    for char in Alphabets:
        attemp_pwd(char, corr_char)

    char_lst = [corr_char[i] for i in sorted(corr_char.keys())]
    pwd = "".join(char_lst) + ",complete"

    output_path = '/home/isl/t2_2/output'
    if not os.path.exists(output_path):
        os.mkdir(output_path)

    print(pwd)

    with open(output_path+'/oput_{}'.format(str(id)), 'w') as file:
        file.write(pwd)

    os.system('rm -rf /home/isl/t2_2/traces')


if __name__ == '__main__':
    main()

