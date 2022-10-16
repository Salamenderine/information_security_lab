package schnorr.reductions;

import java.lang.reflect.AnnotatedTypeVariable;
import java.math.BigInteger;
import java.nio.channels.IllegalChannelGroupException;

import dlog.I_DLog_Challenger;
import genericGroups.IGroupElement;
import schnorr.I_Schnorr_EUFNMA_Adversary;
import schnorr.Schnorr_PK;
import utils.Pair;

// Newly imported
import utils.NumberUtils;
import java.security.SecureRandom;
import java.util.Map;
import java.util.HashMap;

public class Schnorr_EUFNMA_Reduction extends A_Schnorr_EUFNMA_Reduction{

    // Self defined attributes
    private IGroupElement generator, x;
    private HashMap<Pair<String, IGroupElement>, BigInteger> map;
    private Schnorr_PK<IGroupElement> challenge;
    private long seed = 10l;

    public Schnorr_EUFNMA_Reduction(I_Schnorr_EUFNMA_Adversary<IGroupElement, BigInteger> adversary) {
        super(adversary);
        //Do not change this constructor!
    }

    @Override
    public Schnorr_PK<IGroupElement> getChallenge() {
        //Write your Code here!
        // SecureRandom rng = new SecureRandom();
        // BigInteger sk = NumberUtils.getRandomBigInteger(rng, this.generator.getGroupOrder());
        // IGroupElement R = this.generator.power(sk);
        // Schnorr_PK<IGroupElement> pk = new Schnorr_PK<IGroupElement>(this.generator, R);
        return new Schnorr_PK<IGroupElement>(this.generator, this.x);
    }

    @Override
    public BigInteger hash(String message, IGroupElement r) {
        //Write your Code here!
        var pair = new Pair<String, IGroupElement>(message,r);
        if (this.map.containsKey(pair)){
            return this.map.get(pair);
        }
        else{
            SecureRandom rng = new SecureRandom();
            BigInteger hash_result = NumberUtils.getRandomBigInteger(rng, this.generator.getGroupOrder());
            this.map.put(pair, hash_result);
            return hash_result;
        }
    }

    @Override
    public BigInteger run(I_DLog_Challenger<IGroupElement> challenger) {
        //Write your Code here!
        var DL_challenge = challenger.getChallenge();
        this.generator = DL_challenge.generator;
        this.x = DL_challenge.x;

        this.map = new HashMap<>();
        adversary.reset(this.seed);
        var sol1 = adversary.run(this);

        this.map = new HashMap<>();
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
