package katzwang.reductions;

import java.math.BigInteger;

import ddh.I_DDH_Challenger;
import genericGroups.IGroupElement;
import katzwang.A_KatzWang_EUFNMA_Adversary;
import katzwang.KatzWangPK;
import utils.Triple;

import utils.NumberUtils;
import java.security.SecureRandom;
import java.util.HashMap;
// import utils.Pair;

public class KatzWang_EUFNMA_Reduction extends A_KatzWang_EUFNMA_Reduction {

    private IGroupElement generator, x, y, z;
    private HashMap<Triple<IGroupElement, IGroupElement, String>, BigInteger> map;

    public KatzWang_EUFNMA_Reduction(A_KatzWang_EUFNMA_Adversary adversary) {
        super(adversary);
        // Do not change this constructor!
    }

    @Override
    public Boolean run(I_DDH_Challenger<IGroupElement, BigInteger> challenger) {
        var DDH_challenge = challenger.getChallenge();
        this.generator = DDH_challenge.generator;
        this.x = DDH_challenge.x;
        this.y = DDH_challenge.y;
        this.z = DDH_challenge.z;

        this.map = new HashMap<>();

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
        // Write your Code here!
        return new KatzWangPK(this.generator, this.x, this.y, this.z);
    }

    @Override
    public BigInteger hash(IGroupElement comm1, IGroupElement comm2, String message) {
        // Write your Code here!
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


}
