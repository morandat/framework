package org.kevoree.modeling.util.maths.matrix.solvers.decomposition;

import org.kevoree.modeling.util.maths.matrix.DenseMatrix64F;


/**
 * <p>
 * Contains different functions that are useful for computing the QR decomposition of a matrix.
 * </p>
 *
 * <p>
 * Two different families of functions are provided for help in computing reflectors.  Internally
 * both of these functions switch between normalization by division or multiplication.  Multiplication
 * is most often significantly faster than division (2 or 3 times) but produces less accurate results
 * on very small numbers.  It checks to see if round off error is significant and decides which
 * one it should do.
 * </p>
 *
 * <p>
 * Tests were done using the stability benchmark in jmatbench and there doesn't seem to be
 * any advantage to always dividing by the max instead of checking and deciding.  The most
 * noticeable difference between the two methods is with very small numbers.
 * </p>
 *
 * @author Peter Abeles
 */
public class QrHelperFunctions_D64 {

    public static double findMax( double[] u, int startU , int length ) {
        double max = -1;

        int index = startU;
        int stopIndex = startU + length;
        for( ; index < stopIndex; index++ ) {
            double val = u[index];
            val = (val < 0.0D) ? -val : val;
            if( val > max )
                max = val;
        }

        return max;
    }

    public static void divideElements4arg(final int j, final int numRows ,
                                      final double[] u, final double u_0 ) {
//        double div_u = 1.0/u_0;
//
//        if( Double.isInfinite(div_u)) {
        for( int i = j; i < numRows; i++ ) {
            u[i] /= u_0;
        }
//        } else {
//            for( int i = j; i < getNumRows; i++ ) {
//                u[i] *= div_u;
//            }
//        }
    }

    public static void divideElements(int j, int numRows , double[] u, int startU , double u_0 ) {
//        double div_u = 1.0/u_0;
//
//        if( Double.isInfinite(div_u)) {
        for( int i = j; i < numRows; i++ ) {
            u[i+startU] /= u_0;
        }
//        } else {
//            for( int i = j; i < getNumRows; i++ ) {
//                u[i+startU] *= div_u;
//            }
//        }
    }

    public static void divideElements_Brow(int j, int numRows , double[] u,
                                           double b[] , int startB ,
                                           double u_0 ) {
//        double div_u = 1.0/u_0;
//
//        if( Double.isInfinite(div_u)) {
        for( int i = j; i < numRows; i++ ) {
            u[i] = b[i+startB] /= u_0;
        }
//        } else {
//            for( int i = j; i < getNumRows; i++ ) {
//                u[i] = b[i+startB] *= div_u;
//            }
//        }
    }

    public static void divideElements_Bcol(int j, int numRows , int numCols ,
                                           double[] u,
                                           double b[] , int startB ,
                                           double u_0 ) {
//        double div_u = 1.0/u_0;
//
//        if( Double.isInfinite(div_u)) {
        int indexB = j*numCols+startB;
        for( int i = j; i < numRows; i++ , indexB += numCols ) {
            b[indexB] = u[i] /= u_0;
        }
//        } else {
//            int indexB = j*getNumCols+startB;
//            for( int i = j; i < getNumRows; i++ , indexB += getNumCols ) {
//                b[indexB] = u[i] *= div_u;
//            }
//        }
    }

    public static double computeTauAndDivide(int j, int numRows , double[] u, int startU , double max) {
        // compute the norm2 of the matrix, with each element
        // normalized by the max value to avoid overflow problems
        double tau = 0;
//        double div_max = 1.0/max;
//        if( Double.isInfinite(div_max)) {
        // more accurate
        for( int i = j; i < numRows; i++ ) {
            double d = u[startU+i] /= max;
            tau += d*d;
        }
//        } else {
//            // faster
//            for( int i = j; i < getNumRows; i++ ) {
//                double d = u[startU+i] *= div_max;
//                tau += d*d;
//            }
//        }
        tau = Math.sqrt(tau);

        if( u[startU+j] < 0 )
            tau = -tau;

        return tau;
    }

    /**
     * Normalizes elements in 'u' by dividing by max and computes the norm2 of the normalized
     * array u.  Adjust the sign of the returned value depending on the size of the first
     * element in 'u'. Normalization is done to avoid overflow.
     *
     * <pre>
     * for i=j:getNumRows
     *   u[i] = u[i] / max
     *   tau = tau + u[i]*u[i]
     * end
     * tau = sqrt(tau)
     * if( u[j] < 0 )
     *    tau = -tau;
     * </pre>
     *
     * @param j Element in 'u' that it starts at.
     * @param numRows Element in 'u' that it stops at.
     * @param u Array
     * @param max Max value in 'u' that is used to normalize it.
     * @return norm2 of 'u'
     */
    public static double computeTauAndDivide4arg(final int j, final int numRows ,
                                             final double[] u , final double max) {
        double tau = 0;
//        double div_max = 1.0/max;
//        if( Double.isInfinite(div_max)) {
        for( int i = j; i < numRows; i++ ) {
            double d = u[i] /= max;
            tau += d*d;
        }
//        } else {
//            for( int i = j; i < getNumRows; i++ ) {
//                double d = u[i] *= div_max;
//                tau += d*d;
//            }
//        }
        tau = Math.sqrt(tau);

        if( u[j] < 0 )
            tau = -tau;

        return tau;
    }

    /**
     * <p>
     * Performs a rank-1 update operation on the submatrix specified by w with the multiply on the right.<br>
     * <br>
     * A = (I - &gamma;*u*u<sup>T</sup>)*A<br>
     * </p>
     * <p>
     * The order that matrix multiplies are performed has been carefully selected
     * to minimize the number of operations.
     * </p>
     *
     * <p>
     * Before this can become a truly generic operation the submatrix specification needs
     * to be made more generic.
     * </p>
     */
    public static void rank1UpdateMultR( DenseMatrix64F A , double u[] , double gamma ,
                                         int colA0,
                                         int w0, int w1 ,
                                         double _temp[] )
    {
//        for( int i = colA0; i < A.getNumCols; i++ ) {
//            double val = 0;
//
//            for( int k = w0; k < w1; k++ ) {
//                val += u[k]*A.data[k*A.getNumCols +i];
//            }
//            _temp[i] = gamma*val;
//        }

        // reordered to reduce cpu cache issues
        for( int i = colA0; i < A.numCols; i++ ) {
            _temp[i] = u[w0]*A.data[w0 *A.numCols +i];
        }

        for( int k = w0+1; k < w1; k++ ) {
            int indexA = k*A.numCols + colA0;
            double valU = u[k];
            for( int i = colA0; i < A.numCols; i++ ) {
                _temp[i] += valU*A.data[indexA++];
            }
        }
        for( int i = colA0; i < A.numCols; i++ ) {
            _temp[i] *= gamma;
        }

        // end of reorder

        for( int i = w0; i < w1; i++ ) {
            double valU = u[i];

            int indexA = i*A.numCols + colA0;
            for( int j = colA0; j < A.numCols; j++ ) {
                A.data[indexA++] -= valU*_temp[j];
            }
        }
    }

    public static void rank1UpdateMultR8param(DenseMatrix64F A,
                                        double u[], int offsetU,
                                        double gamma,
                                        int colA0,
                                        int w0, int w1,
                                        double _temp[])
    {
//        for( int i = colA0; i < A.getNumCols; i++ ) {
//            double val = 0;
//
//            for( int k = w0; k < w1; k++ ) {
//                val += u[k+offsetU]*A.data[k*A.getNumCols +i];
//            }
//            _temp[i] = gamma*val;
//        }

        // reordered to reduce cpu cache issues
        for( int i = colA0; i < A.numCols; i++ ) {
            _temp[i] = u[w0+offsetU]*A.data[w0 *A.numCols +i];
        }

        for( int k = w0+1; k < w1; k++ ) {
            int indexA = k*A.numCols + colA0;
            double valU = u[k+offsetU];
            for( int i = colA0; i < A.numCols; i++ ) {
                _temp[i] += valU*A.data[indexA++];
            }
        }
        for( int i = colA0; i < A.numCols; i++ ) {
            _temp[i] *= gamma;
        }

        // end of reorder

        for( int i = w0; i < w1; i++ ) {
            double valU = u[i+offsetU];

            int indexA = i*A.numCols + colA0;
            for( int j = colA0; j < A.numCols; j++ ) {
                A.data[indexA++] -= valU*_temp[j];
            }
        }
    }

    /**
     * <p>
     * Performs a rank-1 update operation on the submatrix specified by w with the multiply on the left.<br>
     * <br>
     * A = A(I - &gamma;*u*u<sup>T</sup>)<br>
     * </p>
     * <p>
     * The order that matrix multiplies are performed has been carefully selected
     * to minimize the number of operations.
     * </p>
     *
     * <p>
     * Before this can become a truly generic operation the submatrix specification needs
     * to be made more generic.
     * </p>
     */
    public static void rank1UpdateMultL( DenseMatrix64F A , double u[] ,
                                         double gamma ,
                                         int colA0,
                                         int w0 , int w1 )
    {
        for( int i = colA0; i < A.numRows; i++ ) {
            int startIndex = i*A.numCols+w0;
            double sum = 0;
            int rowIndex = startIndex;
            for( int j = w0; j < w1; j++ ) {
                sum += A.data[rowIndex++]*u[j];
            }
            sum = -gamma*sum;

            rowIndex = startIndex;
            for( int j = w0; j < w1; j++ ) {
                A.data[rowIndex++] += sum*u[j];
            }
        }
    }
}
