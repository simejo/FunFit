package christensenjohnsrud.funfit;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;
//import android.os.Process;

public class sonicPing {
	AudioTrack at;
	AudioRecord ar;
	static int sType = AudioManager.STREAM_MUSIC;
	static int sRate = AudioTrack.getNativeOutputSampleRate(sType);
	static int[] possibleRates = {48000, 44100, 22050, 11025, 8000};
	static int chirpLength;	//milli seconds
	static int chirpPause;	//milli seconds
	static int chirpRepeat;
	static int carrierFreq;	//Hz
	static int bandwidth;	//Hz
	static int bSize;		//Chirp
	static int bResSize; 	//Result
	static int bsSize; 		//Chirp sequence
	static int sPeriod; 	//Chirp sequence period (in shorts)
	static int addRecordLength; //milli seconds
	static int brSize; 		//Recording used buffer size
	static int brSizeInc; 	//Recording true buffer sized for higher minBufferSize demands
	short[] chirp;
	short[] chirp_sequence;
	short[] recording;
	float[] result;
	float[] periodBuffer;
	float distFactor = 1.f;
	float[][] peakList = new float[5][2]; //0/0 = not set. Otherwise [0]->Distance, [1]->Intensity
	boolean first = true;
	boolean camMic = true;
	public int error = 0;
	public int error_detail = 0;
	String TAG = "sonicPing.java";
	
	public sonicPing() {
		this(1, 100, 3000, 2000, 500, 10);
	}
	
	public sonicPing(int msChirpLength, int msChirpPause, int HzCarrierFreq, int HzBandwidth, int msAddRecordLength, int nChirpRepeat) {
		Log.d(TAG, "sonicPing() (constructor)");
		sRate = getMaxRate();
		Log.d(TAG, "sRate = " + sRate);
		if (sRate < 1) {
			error = -1;
			return;
		}

		chirpLength = msChirpLength;
		chirpPause = msChirpPause;
		carrierFreq = HzCarrierFreq;
		chirpRepeat = nChirpRepeat;
		bandwidth = HzBandwidth;
		bSize =  sRate * chirpLength / 1000;
		addRecordLength = msAddRecordLength;
		brSize =  sRate * (addRecordLength+chirpRepeat*(chirpLength+chirpPause)) / 1000;
		brSizeInc = Math.max(brSize, AudioRecord.getMinBufferSize(sRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT)*16); //Ugly fix for Samsung (Suck it!)
		bResSize =  sRate * (chirpPause-2*chirpLength) / 1000;
		sPeriod = sRate * (chirpLength+chirpPause) / 1000;
		bsSize =  chirpRepeat * sPeriod;
		distFactor = 340/(float)sRate/2.f;
		
		Log.d(TAG, "brSize = " + brSize + ", brSizeInc = " + brSizeInc + ", minBufferSize = " + AudioRecord.getMinBufferSize(sRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT));
		
		chirp = new short[bSize];
		chirp_sequence = new short[bsSize];
		buildChirp(chirp, chirp_sequence);
		
		at = new AudioTrack(sType, sRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bsSize*2, AudioTrack.MODE_STATIC);
		if (at == null) {
			error = -2;
			return;
		}
		if (at.write(chirp_sequence, 0, bsSize) < bsSize) {
			error = -3;
			return;
		}
		
		recording = new short[brSizeInc];
		
		result = new float[bResSize];
		periodBuffer = new float[sPeriod];
		
		for (int i = 0; i < 5; i++) {
			peakList[i][0] = 0.f;
			peakList[i][1] = 0.f;
		}
	}
	// checkRate returns true if we can successfully create an AudioRecord object with the given rate
	private boolean checkRate(int rate) {
		Log.d(TAG, "checkRate()");
		int bSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		if ((bSize) < 0) {
			return false;
		}
		return true;
	}
	
	private int getMaxRate() {
		Log.d(TAG, "getMaxRate()");
		int rate = AudioTrack.getNativeOutputSampleRate(sType);
		if (checkRate(rate)) {
			Log.d(TAG, "Checkrate(rate) " + rate);
			return rate;
		}
		for (int i = 0; i < possibleRates.length; i++) {
			rate = possibleRates[i];
			if (checkRate(rate))
				Log.d(TAG, "Checkrate(rate) " + rate);
				return rate;
		}
		Log.d(TAG, "Checkrate rate --> -1");
		return -1;
			
	}
	
	private void buildChirp(short[] buffer, short[] chirp_sequence) {
		Log.d(TAG, "buildChirp()");
		for (int i = 0; i < bSize; i++) {
			//create a sine with sweeping frequency: sin(2 Pi f(t) * t)
			//The sweep goes from the (carrier - bandwidth/2) to (carrier + bandwidth/2): f(t) = carrierFreq + bandwidth*(t/T-0.5)
			//Finally T = bSize / sRate
			//and t = i / sRate
			//The sine is then scaled to the size of "short" and stored in the buffer
			buffer[i] = (short)(Short.MAX_VALUE * Math.sin(2*Math.PI*(carrierFreq + bandwidth*(i/(double)bSize-0.5))*i/(double)sRate));
		}
		for (int i = 0; i < bsSize; i++) {
			if ((i % sPeriod) < bSize)
				chirp_sequence[i] = buffer[i%sPeriod];
			else
				chirp_sequence[i] = 0;
		}
	}
	
	public float[] ping() {
		Log.d(TAG, "ping()");
		int recRes = 0;
		int source;
		ar = null;
		if (camMic) {
			try {
				Log.d(TAG,"Cam Mic == True");
				source = MediaRecorder.AudioSource.CAMCORDER;
				ar = new AudioRecord(source, sRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, brSizeInc*2);
			} catch (Exception e) {
			}
		}
		if (ar == null) {
			try {
				source = MediaRecorder.AudioSource.MIC;
				ar = new AudioRecord(source, sRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, brSizeInc*2);
			} catch (Exception e) {
			}
		}
		if (ar == null) {
			error = -4;
			return null;
		}

		if (!first) {
			Log.d(TAG, "Not the first call, reloading audio data");
			at.reloadStaticData();
		} else {
			Log.d(TAG, "First call, not reloading audio data");
			first = false;
		}
		Log.d(TAG, "Starting recording");
		try {
			ar.startRecording();
		} catch (Exception e) {
			error = -6;
			return null;
		}
		Log.d(TAG, "Starting audio track -> play");
		at.play();
		Log.d(TAG, "Wait for brSize * 1000 / sRate = " + brSize * 1000 / sRate + "ms");
		try {
			this.wait(brSize * 100 / sRate);
		} catch (Exception e) {
			Log.d(TAG, "WARNING: Wait exception!");
		}
		
		Log.d(TAG, "Reading recording buffer");
		int tempRes = 1;
		while (tempRes > 0 && recRes < brSize) {
			tempRes = ar.read(recording, recRes, brSizeInc-recRes);
			recRes += tempRes;
			try {
				this.wait(brSize * 100 / sRate);
			} catch (Exception e) {
				Log.d(TAG, "WARNING: Wait exception!");
			}
		}

		Log.d(TAG, "Stopping recording");
		ar.stop();
		ar.release();
		ar = null;

		Log.d(TAG, "Stopping audio track");
		at.stop();
		
		if (recRes < brSize) {
			Log.d(TAG, "ERROR: Recording buffer smaller than expected!");
			error = -5;
			error_detail = tempRes;
			return null;
		}
		
		Log.d(TAG, "Finding first echo and averaging periods");
		averagePeriod(recording, findBeginning(recording, chirp, addRecordLength * sRate / 1000, bSize), periodBuffer, sPeriod);
		Log.d(TAG, "Calculating cross-correlation");
		crossCorrelate(periodBuffer, chirp, result, bSize, bResSize);
		Log.d(TAG, "Applying Gaussian filter");
		gaussianFilter(result, bResSize, 5);
		Log.d(TAG, "Normalizing result");
		normalize(result, bResSize, 2 * chirpLength * sRate / 1000);
		Log.d(TAG, "---PING() ENDS HERE---");
		return result;
	}
	
	public float[][] getPeakList() {
		Log.d(TAG, "getPeakList()");
		return peakList;
	}
	
	private void crossCorrelate(float[] f, short[] g, float[] res, int gSize, int resSize) { //res has to be of the size fSize-gSize+1 > 0, returns the max of the cross-correlation
		Log.d(TAG, "crossCorrelate()");
		for (int T = 0; T < resSize; T++) {
			res[T] = 0.f;
			if (T < resSize/3) {
				for (int t = 0; t < gSize; t++) {
					res[T] += f[t+T]*g[t];
				}
			}
		}
	}
	
	private void averagePeriod(short[] rec, int zero, float[] pB, int sPeriod) {
		Log.d(TAG, "averagePeriod()");
		for (int i = 0; i < sPeriod; i++) {
			pB[i] = 0.f;
			for (int j = 0; j < chirpRepeat; j++)
				pB[i] += rec[zero+i+j*sPeriod];
		}
	}
	
	private int findBeginning(short[] rec, short[] chirp, int limit, int bSize) {
		Log.d(TAG, "findBeginning()");
		float max = 0.f;
		float temp;
		int i = 0;
		for (int T = 0; T < limit; T++) {
			temp = 0.f;
			for (int t = 0; t < bSize; t++) {
				temp += rec[t+T]*chirp[t];
			}
			if (Math.abs(temp) > max) {
				max = Math.abs(temp);
				i = T;
			}
		}
		return i;
	}

	//Removing noise
	private void gaussianFilter(float[] buffer, int size, int amount) {
		Log.d(TAG, "gaussianFilter()");
		float[] temp = buffer.clone();
		for (int i = 0; i < size/3; i++) {
			buffer[i] = 0;
			for (int j = -2*amount+i; j <= 2*amount+i; j++)
				if (j >= 0 && j < size)
					buffer[i] += Math.abs(temp[j])*Math.exp(-Math.pow((j-i)/amount, 2)/2.f);
		}
	}
	
	private void normalize(float[] buffer, int size, int offset) {
		Log.d(TAG, "normalize()");
		int i, j;
		for (i = 0; i < 5; i++) {
			peakList[i][0] = 0.f;
			peakList[i][1] = 0.f;
		}
		boolean localMax;
		for (i = offset; i < size/3; i++) {
			localMax = true;
			for (j = -bSize; j <= bSize; j++)
				if (buffer[j+i] > buffer[i])
					localMax = false;
			if (localMax) { //The neighboring peaks are smaller - otherwise skip this point
				j = 5;
				while (j > 0 && peakList[j-1][1] < buffer[i]) {
					if (j < 5) {
						peakList[j][0] = peakList[j-1][0];
						peakList[j][1] = peakList[j-1][1];
					}
					j--;
				}
				if (j < 5) {
					peakList[j][0] = i*distFactor;
					peakList[j][1] = buffer[i];
				}
			}
		}
	}
}
