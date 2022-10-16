package cdh_quadratic;

import java.math.BigInteger;
import cdh.CDH_Challenge;
import cdh.I_CDH_Challenger;
import genericGroups.IGroupElement;

/**
 * This is the file you need to implement.
 * 
 * Implement the methods {@code run} and {@code getChallenge} of this class.
 * Do not change the constructor of this class.
 */
public class CDH_Quad_Reduction extends A_CDH_Quad_Reduction<IGroupElement> {

    private IGroupElement generator, x, y, A, B;
    private IGroupElement identity;

    /**
     * Do NOT change or remove this constructor. When your reduction can not provide
     * a working standard constructor, the TestRunner will not be able to test your
     * code and you will get zero points.
     */
    public CDH_Quad_Reduction() {
        // Do not add any code here!
    }

    @Override
    public IGroupElement run(I_CDH_Challenger<IGroupElement> challenger) {
        // This is one of the both methods you need to implement.

        // By the following call you will receive a DLog challenge.
        CDH_Challenge<IGroupElement> challenge = challenger.getChallenge();
        this.generator = challenge.generator;
        this.x = challenge.x;
        this.y = challenge.y;
        this.identity = this.generator.power(BigInteger.valueOf(0));
    
        var gxy = f5(this.x, this.y);
        return gxy;

        // your reduction does not need to be tight. I.e., you may call
        // adversary.run(this) multiple times.

        // Remember that this is a group of prime order p.
        // In particular, we have a^(p-1) = 1 mod p for each a != 0.
    }

    @Override
    public CDH_Challenge<IGroupElement> getChallenge() {

        // This is the second method you need to implement.
        // You need to create a CDH challenge here which will be given to your CDH
        // adversary.
        // Instead of null, your cdh challenge should consist of meaningful group
        // elements.
        CDH_Challenge<IGroupElement> cdh_challenge = new CDH_Challenge<IGroupElement>(this.generator, this.A, this.B);

        return cdh_challenge;
    }

    public IGroupElement f1(IGroupElement A, IGroupElement B){
        // Should return f1(g, g^x, g^y) = g^{axy + bx + cy + d}
        this.A = A;
        this.B = B;
        return adversary.run(this);
    }

    public IGroupElement f2(IGroupElement A, IGroupElement B){
        // return f1(A, B).multiply(f1(this.identity, this.identity).invert());
        var ans1 = f1(A, B);
        var ans2 = f1(this.identity, this.identity);
        return ans1.multiply(ans2.invert());
    }

    public IGroupElement f3(IGroupElement A, IGroupElement B){
        var ans1 = f2(A, B);
        var ans2 = f2(this.identity, B);
        return ans1.multiply(ans2.invert());
    }

    public IGroupElement f4(IGroupElement A, IGroupElement B){
        var ans1 = f3(A, B);
        var ans2 = f3(A, this.identity);
        return ans1.multiply(ans2.invert());
    }

    public IGroupElement f5(IGroupElement A, IGroupElement B){
        var ga = f4(this.generator, this.generator);
        var gaxy = f4(A, B);
        var alpha = this.generator;
        var beta = ga;
        var p_3 = this.generator.getGroupOrder().subtract(BigInteger.valueOf(3));
        
        var bin_str = p_3.toString(2);
        for (int i = 1; i < bin_str.length(); i++){
            if (bin_str.charAt(i) == '1'){
                alpha = f4(alpha, beta);
                beta = f4(beta, beta);
            }
            else{
                beta = f4(alpha, beta);
                alpha = f4(alpha, alpha);
            }
        }
        var gxy = f4(gaxy, beta);

        
        return gxy;
    }
}
