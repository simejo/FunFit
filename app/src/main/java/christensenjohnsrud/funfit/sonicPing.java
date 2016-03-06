package christensenjohnsrud.funfit;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;
//import android.os.Process;

public class sonicPing {
	AudioTrack audioTracker; 	// Manages and plays a single audio resource
	AudioRecord audioRecorder;	// Manages the audio resources to record audio from the audio input hardware
	static int streamType = AudioManager.STREAM_MUSIC;
	static int sampleRate = AudioTrack.getNativeOutputSampleRate(streamType);

	/* Possible rates:
	 * 48000 - Standard audio sampling rate
	 * 44100 - Audio CD
	 * 22050 - Half the rate of Audio CD
	 * 11025 - One quarter of Audio CD
	 * 8000  - Telephone and Walkie-Talkie
	 */

	static int[] possibleRates = {48000, 44100, 22050, 11025, 8000};
	static int chirpLength;			// Milli seconds
	static int chirpPause;			// Milli seconds
	static int chirpRepeat;
	static int carrierFreq;			// The center frequency or the frequency of a carrier wave. Hz
	static int bandwidth;			// The difference between the upper and lower frequencies. Hz
	static int bufferChirpSize;		// Chirp
	static int bufferResultSize; 	// Result
	static int bufferChirpSequenceSize; //Chirp sequence
	static int chirpSequencePeriod; //Chirp sequence period (in shorts)
	static int addRecordLength; 	//milli seconds
	static int bufferRecordSize; 	//Recording used buffer size

	private float speedOfSound = 340.f; //Check for temperature?

	// Use a short to save memory in large arrays, in situations where the memory savings actually matters
	short[] chirp;
	short[] chirp_sequence;
	short[] recordingBuffer;

	float[] result;
	float[] periodBuffer;
	float distanceFactor = 1.f;
	float[][] distanceList = new float[5][2]; //0/0 = not set. Otherwise [0]->Distance, [1]->Probability
	public int error = 0;
	String TAG = "sonicPing.java";
	
	public sonicPing() {
		this(1, 100, 3000, 2000, 500, 10);
	}
	
	public sonicPing(int msChirpLength, int msChirpPause, int HzCarrierFreq, int HzBandwidth, int msAddRecordLength, int nChirpRepeat) {
		Log.d(TAG, "sonicPing() (constructor)");
		sampleRate = getMaxRate();
		Log.d(TAG, "sampleRate = " + sampleRate);
		if (sampleRate < 1) {
			error = -1;
			return;
		}

		chirpLength = msChirpLength;
		chirpPause = msChirpPause;
		carrierFreq = HzCarrierFreq;
		chirpRepeat = nChirpRepeat;
		bandwidth = HzBandwidth;
		addRecordLength = msAddRecordLength;

		bufferChirpSize =  sampleRate * chirpLength / 1000;
		bufferRecordSize =  sampleRate * (addRecordLength+chirpRepeat*(chirpLength+chirpPause)) / 1000;
		bufferResultSize =  sampleRate * (chirpPause-2*chirpLength) / 1000; //WHY chirpPause-2*chirpLength?
		chirpSequencePeriod = sampleRate * (chirpLength+chirpPause) / 1000;
		bufferChirpSequenceSize =  chirpRepeat * chirpSequencePeriod;
		distanceFactor = speedOfSound / (float) sampleRate / 2.f; //Divided by 2 since it travels twice the length
		
		Log.d(TAG, "bufferChirpSize = " + bufferChirpSize +  ", minBufferSize = " + AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT));

		chirp = new short[bufferChirpSize];
		chirp_sequence = new short[bufferChirpSequenceSize];
		buildChirp(chirp, chirp_sequence);
		
		audioTracker = new AudioTrack(streamType, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferChirpSequenceSize *2, AudioTrack.MODE_STATIC);
		audioTracker.write(chirp_sequence, 0, bufferChirpSequenceSize);

		recordingBuffer = new short[bufferRecordSize];
		
		result = new float[bufferResultSize];
		periodBuffer = new float[chirpSequencePeriod];
		
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
		int rate = AudioTrack.getNativeOutputSampleRate(streamType);
		if (checkRate(rate)) {
			return rate;
		}
		for (int i = 0; i < possibleRates.length; i++) {
			rate = possibleRates[i];
			if (checkRate(rate))
				return rate;
		}
		return -1;
			
	}

	// Creates a chirp, a fast frequency sweep from 1000 Hz to 5000 Hz
	private void buildChirp(short[] buffer, short[] chirp_sequence) {
		Log.d(TAG, "buildChirp()");

		for (int i = 0; i < bufferChirpSize; i++) {

			// Create a sine with sweeping frequency: sin(2 Pi f(t) * t)
			// Wiki: sin( 2*Pi ( f0*t + (k/2)*t^2) )
			// The sweep goes from the (carrier - bandwidth/2) to (carrier + bandwidth/2): f(t) = carrierFreq + bandwidth*(t/T-0.5)
			// Finally T = bufferChirpSize / sampleRate
			// and t = i / sampleRate
			// The sine is then scaled to the size of "short" and stored in the buffer
			// Short does not have decimals, that is why we need to multiply the linear chirp with Short.MAX_VALUE

			buffer[i] = (short)(Short.MAX_VALUE * Math.sin(2*Math.PI*(carrierFreq + bandwidth*(i*0.5/(double) bufferChirpSize -0.5))*i/(double) sampleRate));

		}

		// Adding the chirp data in the chirp_sequence
		for (int i = 0; i < bufferChirpSequenceSize; i++) {
			if ((i % chirpSequencePeriod) < bufferChirpSize)
				chirp_sequence[i] = buffer[i% chirpSequencePeriod];
			else
				chirp_sequence[i] = 0;
		}
	}
	
	public float[] ping() {
		Log.d(TAG, "ping()");

		startRecording();
		stopRecording();

		// Finding first echo
		int start = findBeginning(recordingBuffer, chirp, addRecordLength * sampleRate / 1000, bufferChirpSize);
		// Finding averaging periods
		amplifyEchoes(recordingBuffer, start, periodBuffer, chirpSequencePeriod);
		// Calculating cross-correlation
		crossCorrelate(periodBuffer, chirp, result, bufferChirpSize, bufferResultSize);
		// Applying Gaussian filter
		gaussianFilter(result, bufferResultSize, 5);
		// Normalizing result
		normalize(result, bufferResultSize, 2 * chirpLength * sampleRate / 1000);

		return result;
	}

	public void startRecording(){
		// CAMCORDER = Microphone audio source with same orientation as camera if available, the main device microphone otherwise
		audioRecorder = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferRecordSize *2);
		// Sets the playback head position within the static buffer to zero
		audioTracker.reloadStaticData();
		audioRecorder.startRecording();
		audioTracker.play();
		// Reads audio data from the audio hardware for recordingBuffer into a short array
		audioRecorder.read(recordingBuffer, 0, bufferRecordSize);
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

	// 	crossCorrelate(periodBuffer, chirp, result, bufferChirpSize, bufferResultSize);
	private void crossCorrelate(float[] f, short[] g, float[] res, int gSize, int resSize) { //res has to be of the size fSize-gSize+1 > 0, returns the max of the cross-correlation
		//		 crossCorrelate(periodBuffer, chirp, result, bufferChirpSize, bufferResultSize);
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


	private void amplifyEchoes(short[] rec, int zero, float[] pB, int sPeriod) {
		Log.d(TAG, zero+"");
		Log.d(TAG, "amplifyEchoes()");
		for (int i = 0; i < sPeriod; i++) {
			pB[i] = 0.f;
			for (int j = 0; j < chirpRepeat; j++)
				pB[i] += rec[zero+i+j*sPeriod];
		}
	}

	// Finding highest recording value
	// findBeginning(recordingBuffer, chirp, addRecordLength * sampleRate / 1000, bufferChirpSize)
	private int findBeginning(short[] rec, short[] chirp, int limit, int bSize) {
		//		findBeginning(recordingBuffer, chirp, addRecordLength * sampleRate / 1000, bufferChirpSize)
		Log.d(TAG, "findBeginning()");
		float max = 0.f;
		float temp;
		int startIndex = 0;
		for (int start = 0; start < limit; start++) {
			temp = 0.f;
			for (int i = 0; i < bSize; i++) {
				temp += rec[start+i]*chirp[i];
			}
			if (Math.abs(temp) > max) {
				max = Math.abs(temp);
				startIndex = start;
			}
		}
		return startIndex;
	}

	// Removing noise
	private void gaussianFilter(float[] buffer, int size, int amount) {
		//		 gaussianFilter(result, bufferResultSize, 5);
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
			for (int z = -bufferChirpSize; z <= bufferChirpSize; z++)
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
					distanceList[w][0] = y* distanceFactor;
					distanceList[w][1] = buffer[y];
				}
			}
		}
	}
}
