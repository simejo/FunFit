package christensenjohnsrud.funfit;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;
//import android.os.Process;

public class sonicPing {
	AudioTrack audioTracker;
	AudioRecord audioRecorder;
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
	static int brMaxMinBuffer; 	//Recording true buffer sized for higher minBufferSize demands
	short[] chirp;
	short[] chirp_sequence;
	short[] recordingBuffer;
	float[] result;
	float[] periodBuffer;
	float distFactor = 1.f;
	float[][] distanceList = new float[5][2]; //0/0 = not set. Otherwise [0]->Distance, [1]->Probability
	public int error = 0;
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
		brMaxMinBuffer = Math.max(brSize, AudioRecord.getMinBufferSize(sRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT)*16); //Ugly fix for Samsung (Suck it!)
		bResSize =  sRate * (chirpPause-2*chirpLength) / 1000;
		sPeriod = sRate * (chirpLength+chirpPause) / 1000;
		bsSize =  chirpRepeat * sPeriod;
		distFactor = 340/(float)sRate/2.f;
		
		Log.d(TAG, "bSize = " + bSize + ", brMaxMinBuffer = " + brMaxMinBuffer + ", minBufferSize = " + AudioRecord.getMinBufferSize(sRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT));
		
		chirp = new short[bSize];
		chirp_sequence = new short[bsSize];
		buildChirp(chirp, chirp_sequence);
		
		audioTracker = new AudioTrack(sType, sRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bsSize*2, AudioTrack.MODE_STATIC);
		audioTracker.write(chirp_sequence, 0, bsSize);

		recordingBuffer = new short[brMaxMinBuffer];
		
		result = new float[bResSize];
		periodBuffer = new float[sPeriod];
		
		for (int i = 0; i < 5; i++) {
			distanceList[i][0] = 0.f;
			distanceList[i][1] = 0.f;
		}
	}
	// Returns true if we can successfully create an AudioRecord object with the given rate
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

		startRecording();
		stopRecording();

		// Finding first echo and averaging periods
		averagePeriod(recordingBuffer, findBeginning(recordingBuffer, chirp, addRecordLength * sRate / 1000, bSize), periodBuffer, sPeriod);
		// Calculating cross-correlation
		crossCorrelate(periodBuffer, chirp, result, bSize, bResSize);
		// Applying Gaussian filter
		gaussianFilter(result, bResSize, 5);
		// Normalizing result
		normalize(result, bResSize, 2 * chirpLength * sRate / 1000);

		return result;
	}

	public void startRecording(){
		// CAMCORDER = Microphone audio source with same orientation as camera if available, the main device microphone otherwise
		audioRecorder = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, brMaxMinBuffer *2);
		// Sets the playback head position within the static buffer to zero
		audioTracker.reloadStaticData();
		audioRecorder.startRecording();
		audioTracker.play();
		// Reads audio data from the audio hardware for recordingBuffer into a short array
		audioRecorder.read(recordingBuffer, 0, brMaxMinBuffer);
	}

	public void stopRecording(){
		audioRecorder.stop();
		audioRecorder.release();
		audioRecorder = null;
		audioTracker.stop();
	}
	
	public float[][] getDistanceList() {
		Log.d(TAG, "getDistanceList()");
		return distanceList;
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
		Log.d(TAG, zero+"");
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

	// Removing noise
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
	// Find the five most probable distances
	private void normalize(float[] buffer, int size, int offset) {
		Log.d(TAG, "normalize()");
		// Creating empty list
		for (int x = 0; x < 5; x++) {
			distanceList[x][0] = 0.f;
			distanceList[x][1] = 0.f;
		}
		boolean localMax;
		for (int y = offset; y < size/3; y++) {
			localMax = true;
			for (int z = -bSize; z <= bSize; z++)
				if (buffer[z+y] > buffer[y])
					localMax = false;
			if (localMax) { //The neighboring peaks are smaller - otherwise skip this point
				int w = 5;
				while (w > 0 && distanceList[w-1][1] < buffer[y]) {
					Log.d(TAG + " normalize", distanceList[w-1][1] +" < "+ buffer[y]);
					if (w < 5) {
						distanceList[w][0] = distanceList[w-1][0];
						distanceList[w][1] = distanceList[w-1][1];
					}
					w--;
				}
				if (w < 5) {
					distanceList[w][0] = y*distFactor;
					distanceList[w][1] = buffer[y];
				}
			}
		}
	}
}
