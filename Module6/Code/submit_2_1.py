import sys
import os

CMP_TRUE_ADDR = "0x401211"
SHIFT_ADDR = "0x401286"
TO_SHIFT = "0x401292"
CORR_ADDR = "0x4012a8"

def transform(original, shift):
    ascii_idx = ord(original) - ord('a') + shift
    ascii_idx %= 26
    ascii_idx += ord('a')
    return chr(ascii_idx)

def main():
    assert len(sys.argv) == 3, "Requires two arguments: path-to-trace and trace-id"

    trace_path, trace_id = sys.argv[1], sys.argv[2]
    trace_names = os.listdir(trace_path)
    passwords = [item[:-4] for item in trace_names]
    corr_chars, is_corr, pwd_len = {}, False, None

    for pwd in passwords:
        shift, idx = 0, 0
        addr_lst = []
        with open(trace_path + "/{}.txt".format(pwd)) as file:
            for line in file:
                if "E:" in line and ":C:" in line:
                    addr_lst.append(line.split(':')[1])
        for addr in addr_lst:
            if addr == CMP_TRUE_ADDR:
                corr_chars[idx - 1] = pwd[idx - 1]
            elif SHIFT_ADDR == addr:
                shift += 1
            elif addr == TO_SHIFT:
                if shift > 0:
                    corr_chars[idx - 1] = transform(pwd[idx - 1], shift-1)
                shift = 0
                idx += 1
            elif CORR_ADDR == addr:
                is_corr = True
            
        if len(pwd) >= idx:
            pwd_len = idx - 1

    if pwd_len is not None:
        is_corr = True

    final_pwd = ''
    for i in range(max(corr_chars.keys()) + 1):
        if i in corr_chars:
            final_pwd += corr_chars[i]
        else:
            final_pwd += "_"
            is_corr = False

    if is_corr:
        msg = final_pwd + ",complete"
    else:
        msg = final_pwd + ",partial"

    print(msg)

    output_path = '/home/isl/t2_1/output/oput_{}'.format(trace_id)
    if not os.path.exists('/home/isl/t2_1/output'):
        os.mkdir('/home/isl/t2_1/output')
    with open(output_path, 'w') as file:
        file.write(msg)



if __name__ == "__main__":
    main()