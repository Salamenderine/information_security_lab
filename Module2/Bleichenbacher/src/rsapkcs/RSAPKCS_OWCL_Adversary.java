package rsapkcs;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import javax.naming.directory.SearchControls;
import javax.sound.midi.SysexMessage;

import static utils.NumberUtils.getRandomBigInteger;
import static utils.NumberUtils.ceilDivide;
import static utils.NumberUtils.getCeilLog;

import java.util.HashMap;
import utils.Pair;
import java.lang.Math;

public class RSAPKCS_OWCL_Adversary implements I_RSAPKCS_OWCL_Adversary {
    public RSAPKCS_OWCL_Adversary() {
        // Do not change this constructor!
    }

    private BigInteger C, N, e, message, C0, B;
    private int k, i;
    private ArrayList<Pair<BigInteger, BigInteger>> M = new ArrayList<>();
    private ArrayList<BigInteger> s = new ArrayList<>();
    private I_RSAPKCS_OWCL_Challenger challenger;

    /*
     * @see basics.IAdversary#run(basics.IChallenger)
     */
    @Override
    public BigInteger run(final I_RSAPKCS_OWCL_Challenger challenger) {
        // Write code here!
        this.challenger = challenger;
        this.C = challenger.getChallenge();
        var PK = challenger.getPk();
        this.N = PK.N;
        this.e = PK.exponent;
        // this.k = challenger.getPlainTextLength();
        this.k = this.N.toString(2).length() ;
        System.out.println("value of k is " + this.k);
        System.out.println("Value of plaintext length is " + challenger.getPlainTextLength());
        this.B = BigInteger.valueOf(2).pow(this.k - 16);
        System.out.println("Value of B is " + this.B.toString(10));
        System.out.println("Value of N is" + this.N.toString(10));

        // Step 1
        var rng = new Random();
        do{
            var s0 = getRandomBigInteger(rng, this.N);
            var c0 = this.C.multiply(s0.modPow(this.e, this.N)).mod(N);
            if (checkConforming(c0)){
                this.s.add(s0);
                this.C0 = c0;
                this.M.add(new Pair<BigInteger,BigInteger>(this.B.multiply(BigInteger.valueOf(2)), this.B.multiply(BigInteger.valueOf(3)).subtract(BigInteger.ONE)));
                this.i = 1;
                System.out.println("S0 found: " + this.s.get(0));
                break;
            }
        }
        while (true);

        // Loop for step 2,3,4
        do{
            // Step 2: Loop to find conforming si
            // Step 2a
            System.out.println("At round" + this.i);
            System.out.println("M size: " + this.M.size());
            
            if (this.i==1){
                System.out.println("Step 2a");
                var lbound = ceilDivide(this.N, BigInteger.valueOf(3).multiply(this.B));
                // var lbound = ceilDivide(this.N, BigInteger.valueOf(3 * this.B)).intValue();
                this.s.add(searchConform(this.C0, lbound, null));
                System.out.println("S1 found: " + this.s.get(i));
            }
            // Step 2b
            else if (this.i > 1 && this.M.size() >= 2){
                System.out.println("Step 2b");
                var lbound = this.s.get(i-1).add(BigInteger.ONE);
                this.s.add(searchConform(this.C0, lbound, null));
                System.out.println("S" + this.i + " Found: " + this.s.get(this.i));
            }
            // Step 2c
            else{
                System.out.println("Step 2c");
                System.out.println("M contains one interval");
                if (this.M.size() != 1){
                    System.out.println("The size of M is incorrect!");
                }
                var interval = this.M.get(0);
                var a = interval.first;
                var b = interval.second;
                var rlbd = b.multiply(this.s.get(i-1)).subtract(BigInteger.valueOf(2).multiply(this.B));
                rlbd = ceilDivide(BigInteger.valueOf(2).multiply(rlbd), this.N);

                BigInteger prev_slb = null;
                BigInteger prev_sub = null;

                for (BigInteger r = rlbd; true; r = r.add(BigInteger.ONE)){
                    var blb = BigInteger.valueOf(2).multiply(this.B).add(r.multiply(this.N));
                    blb = ceilDivide(blb, b);
                    var bub = BigInteger.valueOf(3).multiply(this.B).add(r.multiply(this.N));
                    bub = floorDivide(bub, a);
                    if (prev_slb != null){
                        if (blb.compareTo(prev_slb)>=0 && blb.compareTo(prev_sub) <=0 && bub.compareTo(prev_sub)>=0){
                            System.out.println("Overlapped");
                            blb = prev_slb.add(BigInteger.ONE);
                        }
                        else if (blb.compareTo(prev_slb)>=0){
                            // System.out.println("No overlap");
                        }
                        else{
                            System.out.println("Assumption about bounds are violated");
                        }

                    }
                    var ss = searchConform(this.C0, blb, bub);
                    if (ss != null){
                        this.s.add(ss);
                        System.out.println("S" + this.i + " Found: " + this.s.get(this.i));
                        break;
                    }
                    prev_slb = blb;
                    prev_sub = bub;
                }

                // for (BigInteger r = rlbd; true; r = r.add(BigInteger.ONE)){
                //     var blb = BigInteger.valueOf(2).multiply(this.B).add(r.multiply(this.N));
                //     blb = ceilDivide(blb, b);
                //     var bub = BigInteger.valueOf(3).multiply(this.B).add(r.multiply(this.N));
                //     bub = floorDivide(bub, a);
                //     var ss = searchConform(this.C0, blb, bub);
                //     if (ss != null){
                //         this.s.add(ss);
                //         System.out.println("S" + this.i + " Found: " + this.s.get(this.i));
                //         break;
                //     }
                // }
            }
            // Step 3
            printM();
            System.out.println("Narowing solution");
            narrowSolution(this.i);

            //Step 4
            System.out.println("Running step4 to find solution");
            if (this.M.size() == 1){
                System.out.println("Only one interval in M");
                var interval = this.M.get(0);
                var a = interval.first;
                var b = interval.second;
                if (b.equals(a)){
                    System.out.println("Message found!");
                    this.message = a.multiply(this.s.get(0).modPow(BigInteger.ONE.negate(), this.N)).mod(this.N);
                    break;
                }
                else if (b.compareTo(a)>0){
                    this.i += 1;
                    System.out.println("One interval left, but a != b");
                }
                else {
                    System.out.println("Something went wrong, the interval has b < a");
                    System.out.println("a: " + a + " b: " + b);
                    this.i += 1;
                }
            }
            else{
                this.i += 1;
            }

        } while (true);

        return this.message;
    }

    public static BigInteger floorDivide(BigInteger numerator, BigInteger denominator) {
        var r = numerator.divideAndRemainder(denominator);
        if (r[1].compareTo(BigInteger.ZERO) == 0) {
            return r[0];
        } else {
            return r[0];
        }
    }

    private BigInteger searchConform(BigInteger c00, BigInteger lowerbound, BigInteger upperbound){
        if (upperbound == null){
            for (BigInteger ss = lowerbound; true; ss = ss.add(BigInteger.ONE)){
                var tempt = c00.multiply(ss.modPow(this.e, this.N)).mod(N);
                if (checkConforming(tempt)){
                    return ss;
                }
            }
        }
        else{
            for (BigInteger ss = lowerbound; ss.compareTo(upperbound) == 0 || ss.compareTo(upperbound) == -1; ss = ss.add(BigInteger.ONE)){
                var tempt = c00.multiply(ss.modPow(this.e, this.N)).mod(N);
                if (checkConforming(tempt)){
                    return ss;
                }
            }
            return null;
        }
    }

    private void narrowSolution(Integer i){
        ArrayList<Pair<BigInteger, BigInteger>> newM = new ArrayList<>();
        for (int j = 0; j < this.M.size(); j++){
            var interval = this.M.get(j);
            var a = interval.first;
            var b = interval.second;
            var rlb = a.multiply(this.s.get(i)).subtract(this.B.multiply(BigInteger.valueOf(3))).add(BigInteger.ONE);
            rlb = ceilDivide(rlb, this.N);
            var rub = b.multiply(this.s.get(i)).subtract(this.B.multiply(BigInteger.valueOf(2)));
            rub = floorDivide(rub, this.N);
            // System.out.println("a: " + a);
            // System.out.println("b: " + b);
            // System.out.println("rub: " + rub);
            // System.out.println("rlb: " + rlb);

            // var new_interval = getIntervalBounds(a, b, rlb, i);
            // printInterval(new_interval);;

            // Code commented out for debugging
            for (BigInteger r = rlb; r.compareTo(rub) <= 0; r = r.add(BigInteger.ONE)){
                var new_interval = getIntervalBounds(a, b, r, i);
                // System.out.println("New interval");
                printInterval(new_interval);
                if (new_interval != null){
                    newM.add(new_interval);
                    }
                }
        }
        this.M = newM;

    }

    private Pair<BigInteger, BigInteger> getIntervalBounds(BigInteger a, BigInteger b, BigInteger r, Integer i){
        var lb = BigInteger.valueOf(2).multiply(this.B).add(r.multiply(this.N));
        lb = ceilDivide(lb, this.s.get(i));
        var ub = BigInteger.valueOf(3).multiply(this.B).subtract(BigInteger.ONE).add(r.multiply(this.N));
        ub = floorDivide(ub, this.s.get(i));
        // ub = ceilDivide(ub, this.s.get(i));
        // System.out.println("lb" + lb.toString(10));
        // System.out.println("a" + a.toString(10));
        // System.out.println("ub" + ub.toString(10));
        // System.out.println("b" + b.toString(10));
        var lower = a.max(lb);
        var upper = b.min(ub);
        // System.out.println("lower" + lower.toString(10));
        // System.out.println("upper" + upper.toString(10));
        if (lower.compareTo(upper) <= 0){
            var pair = new Pair<BigInteger, BigInteger>(lower, upper);
            return pair;
        }
        return null;
    }

    private Boolean checkConforming(BigInteger input){
        try{
            return this.challenger.isPKCSConforming(input);
        }
        catch (Exception e){
            System.out.println("Error found when checking conforming:\n" + e);
            return false;
        }
    }

    private void printM(){
        var size = this.M.size();
        System.out.println("Value of B is " + this.B.toString(10));
        System.out.println("Value of N is " + this.N.toString(10));
        System.out.println("Si: " + this.s.get(this.i));
        for (int j = 0; j<size; j++){
            printInterval(this.M.get(j));
        }
    }

    private void printInterval(Pair<BigInteger, BigInteger> inverval){
        System.out.println("[" + inverval.first + "," + inverval.second + "]");
    }
}