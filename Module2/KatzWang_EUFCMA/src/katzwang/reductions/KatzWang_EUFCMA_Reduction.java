package katzwang.reductions;

import java.math.BigInteger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import ddh.DDH_Challenge;
import ddh.I_DDH_Challenger;
import genericGroups.IGroupElement;
import katzwang.A_KatzWang_EUFCMA_Adversary;
import katzwang.KatzWangPK;
import katzwang.KatzWangSignature;
import katzwang.KatzWangSolution;
import utils.NumberUtils;
import utils.Triple;

import java.security.SecureRandom;

public class KatzWang_EUFCMA_Reduction extends A_KatzWang_EUFCMA_Reduction {

    private IGroupElement generator, x, y, z;
    private HashMap<Triple<IGroupElement, IGroupElement, String>, BigInteger> map;
    private HashMap<String, KatzWangSignature> signatures;

    public KatzWang_EUFCMA_Reduction(A_KatzWang_EUFCMA_Adversary adversary) {
        super(adversary);
        // Do not change this constructor
    }

    @Override
    public Boolean run(I_DDH_Challenger<IGroupElement, BigInteger> challenger) {
        // Implement your code here!
        var DDH_challenge = challenger.getChallenge();
        this.generator = DDH_challenge.generator;
        this.x = DDH_challenge.x;
        this.y = DDH_challenge.y;
        this.z = DDH_challenge.z;

        this.map = new HashMap<>();
        this.signatures = new HashMap<>();

        var solution = adversary.run(this);
        if (solution == null){
            return false;
        }
        var m = solution.message;
        var s = solution.signature.s;
        var c = solution.signature.c;

        var A = this.generator.power(s).multiply(this.y.power(c.negate()));
        var B = this.x.power(s).multiply(this.z.power(c.negate()));
        var hash_back = this.map.get(new Triple(A, B, m));
        return hash_back.equals(c);

    }

    @Override
    public KatzWangPK<IGroupElement> getChallenge() {
        // Implement your code here!
        return new KatzWangPK(this.generator, this.x, this.y, this.z);
    }

    @Override
    public BigInteger hash(IGroupElement comm1, IGroupElement comm2, String message) {
        // Implement your code here!
        var triple = new Triple<>(comm1, comm2, message);
        if (this.map.containsKey(triple)){
            return this.map.get(triple);
        }
        else{
            var rng = new SecureRandom();
            var hash_result = NumberUtils.getRandomBigInteger(rng, this.generator.getGroupOrder());
            this.map.put(triple, hash_result);
            return hash_result;
        }
    }

    @Override
    public KatzWangSignature<BigInteger> sign(String message) {
        // Implement your code here!
        if (this.signatures.containsKey(message)){
            return this.signatures.get(message);
        }
        else{
            var rng = new SecureRandom();
            var c = NumberUtils.getRandomBigInteger(rng, this.generator.getGroupOrder());
            var s = NumberUtils.getRandomBigInteger(rng, this.generator.getGroupOrder());

            var Ap = this.generator.power(s).multiply(this.y.power(c.negate()));
            var Bp = this.x.power(s).multiply(this.z.power(c.negate()));

            var triple = new Triple(Ap, Bp, message);
            var signature = new KatzWangSignature(c, s);
            this.signatures.put(message, signature);
            this.map.put(triple, c);
            return signature;
        }
    }
}
