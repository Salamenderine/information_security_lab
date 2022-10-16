package schnorr.reductions;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;

import dlog.DLog_Challenge;
import dlog.I_DLog_Challenger;
import genericGroups.IGroupElement;
import schnorr.I_Schnorr_EUFCMA_Adversary;
import schnorr.SchnorrSignature;
import schnorr.SchnorrSolution;
import schnorr.Schnorr_PK;
import utils.NumberUtils;
import utils.Pair;

public class Schnorr_EUFCMA_Reduction extends A_Schnorr_EUFCMA_Reduction {

    private IGroupElement generator, x;
    private HashMap<Pair<String, IGroupElement>, BigInteger> map;
    private HashMap<String, SchnorrSignature<BigInteger>> signatures;
    private long seed = 10l;

    public Schnorr_EUFCMA_Reduction(I_Schnorr_EUFCMA_Adversary<IGroupElement, BigInteger> adversary) {
        super(adversary);
        // Do not change this constructor!
    }

    @Override
    public Schnorr_PK<IGroupElement> getChallenge() {
        // Implement your code here!
        return new Schnorr_PK<IGroupElement>(this.generator, this.x);
    }

    @Override
    public SchnorrSignature<BigInteger> sign(String message) {
        // Implement your code here!
        // BigInteger r = NumberUtils.getRandomBigInteger(rng, this.generator.getGroupOrder());
        // var R = this.generator.power(r);
        // var c = hash(message, this.generator);
        // var s = r.add(c.multiply());
        if (this.signatures.containsKey(message)){
            return this.signatures.get(message);
        }
        else{
            SecureRandom rng = new SecureRandom();
            BigInteger r = NumberUtils.getRandomBigInteger(rng, this.generator.getGroupOrder());
            BigInteger t = NumberUtils.getRandomBigInteger(rng, this.generator.getGroupOrder());
            var R = this.generator.power(r).multiply(this.x.power(t.negate()));
            var c = t;
            var s = r;
            var new_signature = new SchnorrSignature<BigInteger>(c, s);
            this.signatures.put(message, new_signature);
            var pair = new Pair<>(message, R);
            this.map.put(pair, c);
            return new_signature;
        }
    }

    @Override
    public BigInteger hash(String message, IGroupElement r) {
        // Implement your code here!
        var pair = new Pair<String, IGroupElement>(message,r);
        if (this.map.containsKey(pair)){
            return this.map.get(pair);
        }
        // else if (this.signatures.containsKey(message)){
        //         var signature = this.signatures.get(message);
        //         var hash_value = signature.c;
        //         this.map.put(pair, hash_value);
        //         return hash_value;
        //     }
        else{
            SecureRandom rng = new SecureRandom();
            BigInteger hash_result = NumberUtils.getRandomBigInteger(rng, this.generator.getGroupOrder());
            this.map.put(pair, hash_result);
            return hash_result;
        }
    }

    @Override
    public BigInteger run(I_DLog_Challenger<IGroupElement> challenger) {
        // Implement your code here!
        var DL_challenge = challenger.getChallenge();
        this.generator = DL_challenge.generator;
        this.x = DL_challenge.x;

        this.map = new HashMap<>();
        this.signatures = new HashMap<>(); 
        adversary.reset(this.seed);
        var sol1 = adversary.run(this);

        this.map = new HashMap<>();
        this.signatures = new HashMap<>(); 
        adversary.reset(this.seed);
        var sol2 = adversary.run(this);

        BigInteger c1 = sol1.signature.c;
        BigInteger s1 = sol1.signature.s;
        BigInteger c2 = sol2.signature.c;
        BigInteger s2 = sol2.signature.s;

        BigInteger numerator = s1.subtract(s2);
        BigInteger denominator = c1.subtract(c2);
        BigInteger solution = numerator.multiply(denominator.modInverse(this.generator.getGroupOrder()));
        solution = solution.mod(this.generator.getGroupOrder());

        return solution;
    }
}
