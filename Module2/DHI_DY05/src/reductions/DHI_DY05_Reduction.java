package reductions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import algebra.SimplePolynomial;
import dhi.DHI_Challenge;
import dhi.I_DHI_Challenger;
import dy05.DY05_PK;
import dy05.I_Selective_DY05_Adversary;
import genericGroups.IGroupElement;

import java.util.HashMap;

public class DHI_DY05_Reduction implements I_DHI_DY05_Reduction {
    // Do not remove this field!
    private final I_Selective_DY05_Adversary adversary;
    private IGroupElement h, PK, generator, identity;
    private int x0, q;
    private HashMap<BigInteger, IGroupElement> signatures = new HashMap<>();
    private SimplePolynomial f;
    private BigInteger order;
    private DHI_Challenge challenge;
    private ArrayList<IGroupElement> betas = new ArrayList<>();
    private ArrayList<BigInteger> gammas = new ArrayList<>();

    public DHI_DY05_Reduction(I_Selective_DY05_Adversary adversary) {
        // Do not change this constructor!
        this.adversary = adversary;
    }

    @Override
    public IGroupElement run(I_DHI_Challenger challenger) {
        // Write Code here!
        this.challenge = challenger.getChallenge();
        this.generator = challenge.get(0);
        this.order = generator.getGroupOrder();
        this.identity = this.generator.power(BigInteger.ZERO);
        this.q = challenge.size() - 1;

        var signature = adversary.run(this);
        var curr = signature;
        for (int j = 0; j<q-1; j++){
            curr = curr.multiply(this.betas.get(j).power(this.gammas.get(j+1).negate()));
        }
        curr = curr.power(this.gammas.get(0).modInverse(order));
        return curr;
    }

    @Override
    public void receiveChallengePreimage(int _challenge_preimage) throws Exception {
        // Write Code here!
        this.x0 = _challenge_preimage;

        this.betas.add(this.generator);
        for (int j = 1; j <= q; j++){
            var betaj = this.identity;
            for (int i = 0; i <= j; i++){
                BigInteger expo = BigInteger.valueOf(binomial(j, i)).multiply(BigInteger.valueOf(-this.x0).pow(j-i));
                expo = expo.mod(this.order);
                betaj = betaj.multiply(this.challenge.get(i).power(expo));
            }
            this.betas.add(betaj);
        }

        this.f = new SimplePolynomial(order, 1);
        for (int j = 0; j<q; j++){
            if (j != this.x0){
                this.f = this.f.multiply(new SimplePolynomial(order, j, 1));
            }
        }

        var prev = f.get(this.q - 1);
        this.gammas.add(f.get(this.q - 1));
        for (int j = 0; j <= this.q - 2; j++){
            var gamma = this.f.get(this.q - 2 - j);
            gamma = gamma.subtract(prev.multiply(BigInteger.valueOf(this.x0)));
            prev = gamma;
            this.gammas.add(0, gamma);
        }

        this.h = generator.power(BigInteger.ZERO);
        this.PK = generator.power(BigInteger.ZERO);
        for (int j = 0; j < q; j++){
            this.h = this.h.multiply(this.betas.get(j).power(f.get(j)));
            this.PK = this.PK.multiply(this.betas.get(j+1).power(f.get(j)));
        }

    }

    @Override
    public IGroupElement eval(int preimage) {
        // Write Code here!
        if (preimage == this.x0){
            return null;
        }
        var big_preimage = BigInteger.valueOf(preimage);
        if (this.signatures.containsKey(big_preimage)){
            return this.signatures.get(big_preimage);
        }
        var denominator = new SimplePolynomial(order, preimage, 1);
        var fi = this.f.div(denominator);
        var sig = this.generator.power(BigInteger.ZERO);
        for (int i = 0; i<this.q-1; i++){
            sig = sig.multiply(this.betas.get(i).power(fi.get(i)));
        }
        this.signatures.put(big_preimage, sig);
        return sig;
        
    }

    @Override
    public DY05_PK getPK() {
        // Write Code here!
        return new DY05_PK(this.h, this.PK);
    }

    static int binomial(final int N, final int K) {
        var ret = 1;
        for (int k = 0; k < K; k++) {
            ret = ret * (N-k) / (k+1);
        }
        return ret;
    }
}
