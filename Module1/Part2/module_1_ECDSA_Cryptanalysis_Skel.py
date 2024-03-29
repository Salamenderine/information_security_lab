import math
import random
from fpylll import LLL
from fpylll import BKZ
from fpylll import IntegerMatrix
from fpylll import CVP
from fpylll import SVP
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec


def egcd(a, b):
    if a == 0:
        return b, 0, 1
    else:
        g, y, x = egcd(b % a, a)
        return g, x - (b // a) * y, y

# Modular inversion computation
def mod_inv(a, p):
    if a < 0:
        return p - mod_inv(-a, p)
    g, x, y = egcd(a, p)
    if g != 1:
        raise ArithmeticError("Modular inverse does not exist")
    else:
        return x % p


def check_x(x, Q):
    """ Given a guess for the secret key x and a public key Q = [x]P,
        checks if the guess is correct.

        :params x:  secret key, as an int
        :params Q:  public key, as a tuple of two ints (Q_x, Q_y)
    """
    x = int(x)
    if x <= 0:
        return False
    Q_x, Q_y = Q
    sk = ec.derive_private_key(x, ec.SECP256R1())
    pk = sk.public_key()
    xP = pk.public_numbers()
    return xP.x == Q_x and xP.y == Q_y

def recover_x_known_nonce(k, h, r, s, q):
    # Implement the "known nonce" cryptanalytic attack on ECDSA
    # The function is given the nonce k, (h, r, s) and the base point order q
    # The function should compute and return the secret signing key x
    r_inv = mod_inv(r, q)
    x = (r_inv * (k * s - h)) % q
    return x

def recover_x_repeated_nonce(h_1, r_1, s_1, h_2, r_2, s_2, q):
    # Implement the "repeated nonces" cryptanalytic attack on ECDSA
    # The function is given the (hashed-message, signature) pairs (h_1, r_1, s_1) and (h_2, r_2, s_2) generated using the same nonce
    # The function should compute and return the secret signing key x
    numerator = (h_1 * s_2 - h_2 * s_1)
    denominator = r_2 * s_1 - r_1 * s_2
    x = (numerator * mod_inv(denominator, q)) % q
    return x


def MSB_to_Padded_Int(N, L, list_k_MSB):
    # Implement a function that does the following: 
    # Let a is the integer represented by the L most significant bits of the nonce k 
    # The function should return a.2^{N - L} + 2^{N -L -1}
    list_k_MSB = [str(i) for i in list_k_MSB]
    k_MSB = ''.join(list_k_MSB)
    a = int(k_MSB, 2)
    return a * 2**(N-L) + 2**(N-L-1)


def LSB_to_Int(list_k_LSB):
    # Implement a function that does the following: 
    # Let a is the integer represented by the L least significant bits of the nonce k 
    # The function should return a
    list_k_LSB = [str(i) for i in list_k_LSB]
    k_LSB = ''.join(list_k_LSB)
    a = int(k_LSB, 2)
    return a

def setup_hnp_single_sample(N, L, list_k_MSB, h, r, s, q, givenbits="msbs", algorithm="ecdsa"):
    # Implement a function that sets up a single instance for the hidden number problem (HNP)
    # The function is given a list of the L most significant bts of the N-bit nonce k, along with (h, r, s) and the base point order q
    # The function should return (t, u) computed as described in the lectures
    # In the case of EC-Schnorr, r may be set to h
    if algorithm == 'ecdsa':
        if givenbits == 'msbs':
            t = (r * mod_inv(s, q)) % q
            z = (h * mod_inv(s, q)) % q
            u = MSB_to_Padded_Int(N, L, list_k_MSB) - z
            return t, u 
        if givenbits == 'lsbs':
            normalizer = mod_inv(2**L, q)
            z = (h * mod_inv(s, q)) % q
            t = (r * mod_inv(s, q) * normalizer) % q
            u = ((LSB_to_Int(list_k_MSB) - z) * normalizer) % q
            return t, u

    if algorithm == 'ecschnorr':
        if givenbits == 'msbs':
            t = h
            u = MSB_to_Padded_Int(N,L, list_k_MSB) - s
            return t, u
        if givenbits == 'lsbs':
            normalizer = mod_inv(2**L, q)
            t = (h * normalizer) % q
            u = ((LSB_to_Int(list_k_MSB) - s) * normalizer) % q
            return t, u


def setup_hnp_all_samples(N, L, num_Samples, listoflists_k_MSB, list_h, list_r, list_s, q, givenbits="msbs", algorithm="ecdsa"):
    # Implement a function that sets up n = num_Samples many instances for the hidden number problem (HNP)
    # For each instance, the function is given a list the L most significant bits of the N-bit nonce k, along with (h, r, s) and the base point order q
    # The function should return a list of t values and a list of u values computed as described in the lectures
    # Hint: Use the function you implemented above to set up the t and u values for each instance
    # In the case of EC-Schnorr, list_r may be set to list_h
    t_lst = [None for i in range(num_Samples)]
    u_lst = [None for i in range(num_Samples)]
    for i in range(num_Samples):
        t, u = setup_hnp_single_sample(N, L, listoflists_k_MSB[i], list_h[i], list_r[i], list_s[i], 
                                        q, givenbits, algorithm)
        t_lst[i] = t
        u_lst[i] = u
    return t_lst, u_lst


def hnp_to_cvp(N, L, num_Samples, list_t, list_u, q):
    # Implement a function that takes as input an instance of HNP and converts it into an instance of the closest vector problem (CVP)
    # The function is given as input a list of t values, a list of u values and the base point order q
    # The function should return the CVP basis matrix B (to be implemented as a nested list) and the CVP target vector u (to be implemented as a list)
    # NOTE: The basis matrix B and the CVP target vector u should be scaled appropriately. Refer lecture slides and lab sheet for more details 
    B = [[0 for i in range(num_Samples + 1)] for j in range(num_Samples+1)]
    for i in range(num_Samples):
        B[i][i] = int(q * 2**(L+1))
        B[-1][i] = int(list_t[i] * 2**(L+1))
    B[-1][-1] = 1
    u = [int(u * 2**(L+1)) for u in list_u] + [0]
    return B, u
    
    

def cvp_to_svp(N, L, num_Samples, cvp_basis_B, cvp_list_u):
    # Implement a function that takes as input an instance of CVP and converts it into an instance of the shortest vector problem (SVP)
    # Your function should use the Kannan embedding technique in the lecture slides
    # The function is given as input a CVP basis matrix B and the CVP target vector u
    # The function should use the Kannan embedding technique to output the corresponding SVP basis matrix B' of apropriate dimensions.
    # The SVP basis matrix B' should again be implemented as a nested list

    # Rescaling:
    # if cvp_basis_B[-1][-1] == 1 / 2**(L+1):
    cvp_basis_B = [[entry * 2**(L+1) for entry in cvp_basis_B[i]] for i in range(num_Samples+1)]
    cvp_basis_B[-1][-1] = 1
    cvp_list_u = [u * 2**(L+1) for u in cvp_list_u]

    # B = IntegerMatrix.from_matrix(cvp_basis_B)
    # LLL.reduction(B)
    M = 2**N
    B_svp = [list(cvp_basis_B[i]) + [0] for i in range(len(cvp_basis_B))] + [cvp_list_u + [M]]
    return B_svp


def solve_cvp(cvp_basis_B, cvp_list_u):
    # Implement a function that takes as input an instance of CVP and solves it using in-built CVP-solver functions from the fpylll library
    # The function is given as input a CVP basis matrix B and the CVP target vector u
    # The function should output the solution vector v (to be implemented as a list)
    # NOTE: The basis matrix B should be processed appropriately before being passes to the fpylll CVP-solver. See lab sheet for more details
    B = IntegerMatrix.from_matrix(cvp_basis_B)
    B = LLL.reduction(B)
    v = CVP.closest_vector(B, cvp_list_u)
    return list(v)

def solve_svp(svp_basis_B):
    # Implement a function that takes as input an instance of SVP and solves it using in-built SVP-solver functions from the fpylll library
    # The function is given as input the SVP basis matrix B
    # The function should output a list of candidate vectors that may contain x as a coefficient
    # NOTE: Recall from the lecture and also from the exercise session that for ECDSA cryptanalysis based on partial nonces, you might want
    #       your function to include in the list of candidate vectors the *second* shortest vector (or even a later one). 
    # If required, figure out how to get the in-built SVP-solver functions from the fpylll library to return the second (or later) shortest vector
    B = IntegerMatrix.from_matrix(svp_basis_B)
    sv = SVP.shortest_vector(B)
    v1 = list(B[0])
    v2 = list(B[1])
    return [sv, v1, v2]


def recover_x_partial_nonce_CVP(Q, N, L, num_Samples, listoflists_k_MSB, list_h, list_r, list_s, q, givenbits="msbs", algorithm="ecdsa"):
    # Implement the "repeated nonces" cryptanalytic attack on ECDSA and EC-Schnorr using the in-built CVP-solver functions from the fpylll library
    # The function is partially implemented for you. Note that it invokes some of the functions that you have already implemented
    list_t, list_u = setup_hnp_all_samples(N, L, num_Samples, listoflists_k_MSB, list_h, list_r, list_s, q, givenbits, algorithm)
    cvp_basis_B, cvp_list_u = hnp_to_cvp(N, L, num_Samples, list_t, list_u, q)
    v_List = solve_cvp(cvp_basis_B, cvp_list_u)
    # The function should recover the secret signing key x from the output of the CVP solver and return it
    x = v_List[num_Samples] % q
    if check_x(x, Q):
        return x
    else:
        raise Exception("CVP did not find solution!")



def recover_x_partial_nonce_SVP(Q, N, L, num_Samples, listoflists_k_MSB, list_h, list_r, list_s, q, givenbits="msbs", algorithm="ecdsa"):
    # Implement the "repeated nonces" cryptanalytic attack on ECDSA and EC-Schnorr using the in-built CVP-solver functions from the fpylll library
    # The function is partially implemented for you. Note that it invokes some of the functions that you have already implemented
    list_t, list_u = setup_hnp_all_samples(N, L, num_Samples, listoflists_k_MSB, list_h, list_r, list_s, q, givenbits, algorithm)
    cvp_basis_B, cvp_list_u = hnp_to_cvp(N, L, num_Samples, list_t, list_u, q)
    svp_basis_B = cvp_to_svp(N, L, num_Samples, cvp_basis_B, cvp_list_u)
    list_of_f_List = solve_svp(svp_basis_B)
    # The function should recover the secret signing key x from the output of the SVP solver and return it
    for fp in list_of_f_List:
        x = (- fp[num_Samples]) % q
        if check_x(x, Q):
            return x
    raise Exception('SVP found no solution!')



# testing code: do not modify

from module_1_ECDSA_Cryptanalysis_tests import run_tests

run_tests(recover_x_known_nonce,
    recover_x_repeated_nonce,
    recover_x_partial_nonce_CVP,
    recover_x_partial_nonce_SVP
)
