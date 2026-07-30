package constracq.core.acqconstraint;

/**
 *
 * @author agutierr
 */
public abstract class UnaryConstraint extends ACQ_Constraint{

    public UnaryConstraint(String name,int var) {
        super(name,new int[]{var});
    }

//    protected abstract boolean check(int value);

    @Override
	public boolean check(int... value) {
        return check(value[0]);
    }
    protected abstract boolean check(int value);
}
