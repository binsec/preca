package constracq.core.acqsolver;
/**
 * 
 * Interface used to define the main function of variables domain
 *
 */
public interface ACQ_IDomain {
	/**
	 * Define the lower limit of this domain
	 * 
	 * @param numvar int used to identify the variable
	 * @return The lower limit of this domain
	 */
    public int getMin(int numvar);
    
    /**
	 * Define the upper limit of this domain
	 * 
	 * @param numvar int used to identify the variable
	 * @return The upper limit of this domain
	 */
    public int getMax(int numvar);
}
