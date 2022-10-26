package rsapkcs;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import javax.lang.model.util.Elements.Origin;
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
        this.k = this.N.toString(2).length();
        var K = ceilDivide(BigInteger.valueOf(this.k), BigInteger.valueOf(8));
        this.B = BigInteger.valueOf(2).pow(8 * (K.intValue() - 2));

        // Step 1
        var rng = new Random();
        do{
            var s0 = getRandomBigInteger(rng, this.N);
            var c0 = this.C.multiply(s0.modPow(this.e, this.N)).mod(this.N);
            if (checkConforming(c0)){
                this.s.add(s0);
                this.C0 = c0;
                this.M.add(new Pair<BigInteger,BigInteger>(this.B.multiply(BigInteger.valueOf(2)), this.B.multiply(BigInteger.valueOf(3)).subtract(BigInteger.ONE)));
                this.i = 1;
                break;
            }
        }
        while (true);

        // Loop for step 2,3,4
        do{
            // Step 2: Loop to find conforming si
            // Step 2a
            
            if (this.i==1){
                var lbound = ceilDivide(this.N, BigInteger.valueOf(3).multiply(this.B));
                // var lbound = ceilDivide(this.N, BigInteger.valueOf(3 * this.B)).intValue();
                this.s.add(searchConform(this.C0, lbound, null));
            }
            // Step 2b
            else if (this.i > 1 && this.M.size() >= 2){
                var lbound = this.s.get(i-1).add(BigInteger.ONE);
                this.s.add(searchConform(this.C0, lbound, null));
            }
            // Step 2c
            else{
                if (this.M.size() != 1){
                    System.out.println("The size of M is incorrect!");
                }
                var interval = this.M.get(0);
                var a = interval.first;
                var b = interval.second;
                var rlbd = b.multiply(this.s.get(i-1)).subtract(BigInteger.valueOf(2).multiply(this.B));
                rlbd = ceilDivide(BigInteger.valueOf(2).multiply(rlbd), this.N);

                for (BigInteger r = rlbd; true; r = r.add(BigInteger.ONE)){
                    var blb = BigInteger.valueOf(2).multiply(this.B).add(r.multiply(this.N));
                    blb = ceilDivide(blb, b);
                    var bub = BigInteger.valueOf(3).multiply(this.B).add(r.multiply(this.N));
                    if (!checkDivides(bub, a)){
                        bub = floorDivide(bub, a);
                    }
                    else{
                        bub = floorDivide(bub, a).subtract(BigInteger.ONE);
                    }
                    
                    var ss = searchConform(this.C0, blb, bub);
                    if (ss != null){
                        this.s.add(ss);
                        break;
                    }
                }
            }
            // Step 3
            narrowSolution(this.i);

            //Step 4
            if (this.M.size() == 1){
                var interval = this.M.get(0);
                var a = interval.first;
                var b = interval.second;
                if (b.equals(a)){
                    this.message = a.multiply(this.s.get(0).modInverse(this.N)).mod(this.N);
                    break;
                }
                else if (b.compareTo(a)>0){
                    this.i += 1;
                }
                else {
                    this.i += 1;
                }
            }
            else{
                this.i += 1;
            }

        } while (true);
        var textL = challenger.getPlainTextLength() * 8;
        var msg_str = this.message.toString(2);
        var msg = msg_str.substring(msg_str.length()-textL, msg_str.length());
        var original_mst = new BigInteger(msg, 2);
        return original_mst;
    }

    public static BigInteger floorDivide(BigInteger numerator, BigInteger denominator) {
        var r = numerator.divideAndRemainder(denominator);
        if (r[1].compareTo(BigInteger.ZERO) == 0) {
            return r[0];
        } else {
            return r[0];
        }
    }

    public static Boolean checkDivides(BigInteger numerator, BigInteger denominator){
        var r = numerator.divideAndRemainder(denominator);
        return r[1].compareTo(BigInteger.ZERO) == 0;
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
            // Code commented out for debugging
            for (BigInteger r = rlb; r.compareTo(rub) <= 0; r = r.add(BigInteger.ONE)){
                var new_interval = getIntervalBounds(a, b, r, i);
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
        var lower = a.max(lb);
        var upper = b.min(ub);
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