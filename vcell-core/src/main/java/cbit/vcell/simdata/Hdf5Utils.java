package cbit.vcell.simdata;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import cbit.vcell.math.Variable;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

public class Hdf5Utils {
	private static final String HDF5_GROUP_SOLUTION = "/solution";
	private static final String HDF5_GROUP_EXTRAPOLATED_VOLUMES = "/extrapolated_volumes";
	private static final String HDF5_GROUP_DIRECTORY_SEPARATOR = "/";

	public static String getVarSolutionPath(String varName)
	{
		return HDF5_GROUP_SOLUTION + HDF5_GROUP_DIRECTORY_SEPARATOR + Variable.getNameFromCombinedIdentifier(varName); 
	}
	
	public static String getVolVarExtrapolatedValuesPath(String varName)
	{
		return HDF5_GROUP_EXTRAPOLATED_VOLUMES + HDF5_GROUP_DIRECTORY_SEPARATOR + "__" + Variable.getNameFromCombinedIdentifier(varName) + "_extrapolated__"; 
	}

	public static class HDF5WriteHelper {
		public int hdf5DataSpaceID;
		public int hdf5DatasetValuesID;
		public HDF5WriteHelper(int hdf5DataSpaceID, int hdf5DatasetValuesID) {
			super();
			this.hdf5DataSpaceID = hdf5DataSpaceID;
			this.hdf5DatasetValuesID = hdf5DatasetValuesID;
		}
		public void close() throws HDF5LibraryException {
			H5.H5Sclose(hdf5DataSpaceID);
			H5.H5Dclose(hdf5DatasetValuesID);
		}
	}
	public static HDF5WriteHelper createDataset(int hdf5GroupID,String dataspaceName,long[] dims) throws HDF5Exception{
		//Create dataset and return it, must be closed when finished
		long[] datasetDimensions = dims;
		int hdf5DataspaceIDValues = H5.H5Screate_simple(datasetDimensions.length, datasetDimensions, null);
		int hdf5DatasetIDValues = H5.H5Dcreate(hdf5GroupID, dataspaceName,HDF5Constants.H5T_NATIVE_DOUBLE, hdf5DataspaceIDValues,HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
		return new HDF5WriteHelper(hdf5DataspaceIDValues,hdf5DatasetIDValues);
	}
	public static int createGroup(int hdf5GroupID,String dataspaceName) throws HDF5Exception{
		return H5.H5Gcreate(hdf5GroupID, (String)dataspaceName,HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
	}
	public static void copySlice(int copyToDataSet,double[] copyFromData,long[] copyToStart,long[] copyToLength,long[] copyFromDims,long[] copyFromStart,long[] copyFromLength,int dataspaceID) throws NullPointerException, IllegalArgumentException, HDF5Exception {
		int hdf5DataspaceIDSlice = H5.H5Screate_simple(copyFromDims.length, copyFromDims, null);
		//Select the generated sliceData to copy-from
		H5.H5Sselect_hyperslab(hdf5DataspaceIDSlice, HDF5Constants.H5S_SELECT_SET, copyFromStart, null, copyFromLength, null);
		//Select next section of destination to copy-to
		H5.H5Sselect_hyperslab(dataspaceID, HDF5Constants.H5S_SELECT_SET, copyToStart, null, copyToLength,null);
		//Copy from extracted sliceData to hdf5 file dataset
		H5.H5Dwrite_double(copyToDataSet, HDF5Constants.H5T_NATIVE_DOUBLE, hdf5DataspaceIDSlice, dataspaceID, HDF5Constants.H5P_DEFAULT, copyFromData);
		H5.H5Sselect_none(dataspaceID);
		H5.H5Sclose(hdf5DataspaceIDSlice);
	}
	public static void insertAttribute(int hdf5GroupID,String dataspaceName,String data) throws NullPointerException, HDF5Exception {
		//insertAttributes(hdf5GroupID, dataspaceName, new ArrayList<String>(Arrays.asList(new String[] {data})));
		//String[] attr = data.toArray(new String[0]);

		String attr = data + '\u0000';

		//https://support.hdfgroup.org/ftp/HDF5/examples/misc-examples/vlstra.c
		int h5attrcs1 = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
		H5.H5Tset_size (h5attrcs1, attr.length() /*HDF5Constants.H5T_VARIABLE*/);
		int dataspace_id = -1;
		//dataspace_id = H5.H5Screate_simple(dims.length, dims,null);
		dataspace_id = H5.H5Screate(HDF5Constants.H5S_SCALAR);
		int attribute_id = H5.H5Acreate(hdf5GroupID, dataspaceName, h5attrcs1, dataspace_id, HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
		H5.H5Awrite(attribute_id, h5attrcs1, attr.getBytes());
		H5.H5Sclose(dataspace_id);
		H5.H5Aclose(attribute_id);
		H5.H5Tclose(h5attrcs1);
	}
	
	public static void insertAttributes(int hdf5GroupID,String dataspaceName,List<String> data) throws NullPointerException, HDF5Exception {
		String[] attr = data.toArray(new String[0]);
		long[] dims = new long[] {attr.length}; // Always an array of length == 1
		StringBuffer sb = new StringBuffer();
		int MAXSTRSIZE=  -1;

		// Get the max length of all the data strings
		for(int i=0;i<attr.length;i++) {
			int len = attr[i] == null ? -1 : attr[i].length(); // passing a 0 causes null exception
			if (attr[i] == null) attr[i] = "";
			MAXSTRSIZE = Math.max(MAXSTRSIZE, len);
		}

		// Append data to single string buffer, padding with null characters to create uniformity.
		for(int i=0;i<attr.length;i++) {
			sb.append(attr[i]);
			for(int j=0;j<(MAXSTRSIZE-attr[i].length());j++) {
				sb.append('\u0000');//null terminated string for hdf5 native code
			}
		}

		//https://support.hdfgroup.org/ftp/HDF5/examples/misc-examples/vlstra.c
		int h5attrcs1 = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
		H5.H5Tset_size (h5attrcs1, MAXSTRSIZE/*HDF5Constants.H5T_VARIABLE*/);
		int dataspace_id = -1;
		dataspace_id = H5.H5Screate_simple(dims.length, dims,null);
		int attribute_id = H5.H5Acreate(hdf5GroupID, dataspaceName, h5attrcs1, dataspace_id, HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
		H5.H5Awrite(attribute_id, h5attrcs1, sb.toString().getBytes());
		H5.H5Sclose(dataspace_id);
		H5.H5Aclose(attribute_id);
		H5.H5Tclose(h5attrcs1);
	}
	private static ByteBuffer getByteBuffer(byte[] bytes, int index, int times) {
		return ByteBuffer.wrap(bytes, index * times, times).order(ByteOrder.LITTLE_ENDIAN);
	}
	private static byte[] byteArray(double[] doubleArray) {
		int times = Double.SIZE / Byte.SIZE;
		byte[] bytes = new byte[doubleArray.length * times];
		for (int i = 0; i < doubleArray.length; i++) {
			getByteBuffer(bytes, i, times).putDouble(doubleArray[i]);
		}
		return bytes;
	}
	public static void insertAttributes(int hdf5GroupID,String dataspaceName,double[] data) throws NullPointerException, HDF5Exception {
		long[] dims = new long[] {data.length};
		StringBuffer sb = new StringBuffer();
		//https://support.hdfgroup.org/ftp/HDF5/examples/misc-examples/vlstra.c
		int dataspace_id = H5.H5Screate_simple(dims.length, dims,null);
		int attribute_id = H5.H5Acreate(hdf5GroupID, dataspaceName, HDF5Constants.H5T_NATIVE_DOUBLE, dataspace_id, HDF5Constants.H5P_DEFAULT,HDF5Constants.H5P_DEFAULT);
		H5.H5Awrite (attribute_id, HDF5Constants.H5T_NATIVE_DOUBLE, byteArray(data));
		H5.H5Sclose(dataspace_id);
		H5.H5Aclose(attribute_id);
	}
//	public static void insertString(int hdf5GroupID,String dataspaceName,long[] dims,String data) throws NullPointerException, HDF5Exception {
//		insertStrings(hdf5GroupID, dataspaceName, dims,new ArrayList<String>(Arrays.asList(new String[] {data})));
//	}
	public static void insertStrings(int hdf5GroupID,String dataspaceName,long[] dims,List<String> data) throws NullPointerException, HDF5Exception {
		int largestStrLen = 0;
		for(int i=0;i<data.size();i++) {
			largestStrLen = Math.max(largestStrLen, data.get(i).length());
		}
		byte[] bytes = new byte[largestStrLen*data.size()];
		int index = 0;
		for(int i=0;i<data.size();i++) {
			System.arraycopy(data.get(i).getBytes(), 0, bytes, index, data.get(i).length());
			index+= largestStrLen;
		}
		int h5tcs1 = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
		H5.H5Tset_size(h5tcs1, largestStrLen);
		int hdf5DataspaceID = H5.H5Screate_simple(dims.length, dims, null);
		int hdf5DatasetID = H5.H5Dcreate(hdf5GroupID, dataspaceName,h5tcs1, hdf5DataspaceID,HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
		//final byte[] bytes = allStringSB.toString().getBytes();
		H5.H5Dwrite(hdf5DatasetID, h5tcs1, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, bytes);
		H5.H5Tclose(h5tcs1);
		H5.H5Dclose(hdf5DatasetID);
		H5.H5Sclose(hdf5DataspaceID);
	}
	public static void insertInts(int hdf5GroupID,String dataspaceName,long[] dims,int[] data) throws NullPointerException, HDF5Exception {
		int hdf5DataspaceID = H5.H5Screate_simple(dims.length, dims, null);
		int hdf5DatasetID = H5.H5Dcreate(hdf5GroupID, dataspaceName,HDF5Constants.H5T_NATIVE_INT, hdf5DataspaceID,HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
		H5.H5Dwrite_int(hdf5DatasetID, HDF5Constants.H5T_NATIVE_INT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, (int[])data);
		H5.H5Dclose(hdf5DatasetID);
		H5.H5Sclose(hdf5DataspaceID);
	}
	public static void insertDoubles(int hdf5GroupID,String dataspaceName,long[] dims,double[] data) throws NullPointerException, HDF5Exception {
		int hdf5DataspaceID = H5.H5Screate_simple(dims.length, dims, null);
		int hdf5DatasetID = H5.H5Dcreate(hdf5GroupID, dataspaceName,HDF5Constants.H5T_NATIVE_DOUBLE, hdf5DataspaceID,HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
		H5.H5Dwrite_double(hdf5DatasetID, HDF5Constants.H5T_NATIVE_DOUBLE, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, (double[])data);
		H5.H5Dclose(hdf5DatasetID);
		H5.H5Sclose(hdf5DataspaceID);
	}
	public static void insertDoubles(int hdf5GroupID,String dataspaceName,long[] dims,ArrayList<Double> data) throws NullPointerException, HDF5Exception {
		double[] hdfData = ArrayUtils.toPrimitive(((ArrayList<Double>)data).toArray(new Double[0]));
		int hdf5DataspaceID = H5.H5Screate_simple(dims.length, dims, null);
		int hdf5DatasetID = H5.H5Dcreate(hdf5GroupID, dataspaceName,HDF5Constants.H5T_NATIVE_DOUBLE, hdf5DataspaceID,HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
		H5.H5Dwrite_double(hdf5DatasetID, HDF5Constants.H5T_NATIVE_DOUBLE, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, hdfData);
		H5.H5Dclose(hdf5DatasetID);
		H5.H5Sclose(hdf5DataspaceID);
	}
}
