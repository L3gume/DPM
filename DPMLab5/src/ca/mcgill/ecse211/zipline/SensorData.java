package ca.mcgill.ecse211.zipline;

public class SensorData {
  private final int LL_DATA_SIZE = 20;
  private final int US_DATA_SIZE = 20;

  // Reference counts of other objects accessing sensor data
  private int llRefs;
  private int usRefs;

  // Locks
  private Object llRefsLock;
  private Object usRefsLock;
  private Object llDataLock;
  private Object usDataLock;
  private Object llDataDerivLock;
  private Object usDataDerivLock;
  private Object llStatsLock;
  private Object usStatsLock;

  // Circular arrays holding the original sensor data
  private float llData[];
  private float usData[];

  // Circular arrays holding the derivative of the sensor data
  private float llDataDeriv[];
  private float usDataDeriv[];

  // The next index at which data should be placed in the circular arrays
  private int llIndex;
  private int usIndex;

  // Boolean values signalling whether circular arrays are filled
  private boolean llFilled;
  private boolean usFilled;

  //
  // The moving statistics of the values in each circular array
  //
  // Index:
  //
  // 0 - moving average
  // 1 - moving variance
  // 2 - moving standard deviation
  //
  private float[] llStats;
  private float[] usStats;

  /**
   * Constructor
   */
  public SensorData() {
    this.llRefs = 0;
    this.usRefs = 0;

    this.llData = new float[LL_DATA_SIZE];
    this.usData = new float[US_DATA_SIZE];
    this.llDataDeriv = new float[LL_DATA_SIZE];
    this.usDataDeriv = new float[US_DATA_SIZE];

    this.llIndex = 0;
    this.usIndex = 0;

    this.llFilled = false;
    this.usFilled = false;

    this.llStats = new float[] { 0.0f, 0.0f, 0.0f };
    this.usStats = new float[] { 0.0f, 0.0f, 0.0f };
  }

  /**
   * Handler method to be called by an ColorPoller object.
   *
   * @param value the latest data value returned by the color sensor
   */
  public void lightLevelHandler(float value) {
    synchronized (this.llDataLock) {
      // Update moving statistics.
      synchronized (this.llStatsLock) {
        this.updateMovingStatistics(this.llStats, this.llData, this.llIndex, value);
      }

      // Insert latest sample.
      this.llData[this.llIndex] = value;

      // Insert latest sample derivative.
      synchronized (this.llDataDerivLock) {
        float lastValue = this.llData[(this.llIndex - 1 + LL_DATA_SIZE) % LL_DATA_SIZE];
        this.llDataDeriv[this.llIndex] = value - lastValue;
      }

      this.llIndex += 1;
      this.llIndex %= this.LL_DATA_SIZE;
      if (!(this.llFilled) && this.llIndex == 0) {
        // Our circular array is now filled.
        this.llFilled = true;
      }
    }
  }

  /**
   * Handler method to be called by an UltrasonicPoller object.
   *
   * @param value the latest data value returned by the ultrasonic sensor
   */
  public void ultrasonicHandler(float value) {
    synchronized (this.usDataLock) {
      // Update moving statistics.
      synchronized (this.usStatsLock) {
        this.updateMovingStatistics(this.usStats, this.usData, this.usIndex, value);
      }

      // Insert latest sample.
      this.usData[this.usIndex] = value;

      // Insert latest sample derivative.
      synchronized (this.usDataDerivLock) {
        float lastValue = this.usData[(this.usIndex - 1 + US_DATA_SIZE) % US_DATA_SIZE];
        this.usDataDeriv[this.usIndex] = value - lastValue;
      }

      this.usIndex += 1;
      this.usIndex %= this.US_DATA_SIZE;
      if (!(this.usFilled) && this.usIndex == 0) {
        // Our circular array is now filled.
        this.usFilled = true;
      }
    }
  }

  /**
   * Get the number of external objects which access the color sensor data.
   *
   * @return the number of external objects which use the color sensor data provided by this object
   */
  public int getLLRefs() {
    return this.llRefs;
  }

  /**
   * Get the number of external objects which access the ultrasonic sensor data.
   *
   * @return the number of external objects which use the ultrasonic sensor data provided by this object
   */
  public int getUSRefs() {
    return this.usRefs;
  }

  /**
   * Get a copy of the original color sensor data.
   *
   * @return a float array holding a copy of the original color sensor data
   */
  public float[] getLLData() {
    float[] data = null;
    if (this.llFilled) {
      synchronized (this.llDataLock) {
        data = this.llData.clone();
      }
    }
    return data;
  }

  /**
   * Get the latest data value polled from the color sensor.
   *
   * @return the latest color sensor data value
   */
  public float getLLDataLatest() {
    float value;
    // We can safely assume that at least one value has been recorded.
    synchronized (this.llDataLock) {
      value = this.llData[(this.llIndex - 1 + LL_DATA_SIZE) % LL_DATA_SIZE];
    }
    return value;
  }

  /**
   * Get a copy of the original ultrasonic sensor data.
   *
   * @return a float array holding a copy of the original ultrasonic sensor data
   */
  public float[] getUSData() {
    float[] data = null;
    if (this.usFilled) {
      synchronized (this.usDataLock) {
        data = this.usData.clone();
      }
    }
    return data;
  }

  /**
   * Get the latest data value polled from the ultrasonic sensor.
   *
   * @return the latest ultrasonic sensor data value
   */
  public float getUSDataLatest() {
    float value;
    // We can safely assume that at least one value has been recorded.
    synchronized (this.usDataLock) {
      value = this.usData[(this.usIndex - 1 + US_DATA_SIZE) % US_DATA_SIZE];
    }
    return value;
  }

  /**
   * Get a copy of the derivate of the color sensor data.
   *
   * @return a float array holding a copy of the derive of the original color sensor data
   */
  public float[] getLLDataDeriv() {
    float[] data = null;
    if (this.llFilled) {
      synchronized (this.llDataDerivLock) {
        data = this.llDataDeriv.clone();
      }
    }
    return data;
  }

  /**
   * Get the latest derivate of the data polled from the color sensor.
   *
   * @return the latest derivative of the color sensor data
   */
  public float getLLDataDerivLatest() {
    float value;
    // We can safely assume that at least one value has been recorded.
    synchronized (this.llDataDerivLock) {
      value = this.llDataDeriv[(this.llIndex - 1 + LL_DATA_SIZE) % LL_DATA_SIZE];
    }
    return value;
  }

  /**
   * Get a copy of the derivate of the ultrasonic sensor data.
   *
   * @return a float array holding a copy of the derive of the original ultrasonic sensor data
   */
  public float[] getUSDataDeriv() {
    float[] data = null;
    if (this.usFilled) {
      synchronized (this.usDataDerivLock) {
        data = this.usDataDeriv.clone();
      }
    }
    return data;
  }

  /**
   * Get the latest derivate of the data polled from the ultrasonic sensor.
   *
   * @return the latest derivative of the ultrasonic sensor data
   */
  public float getUSDataDerivLatest() {
    float value;
    // We can safely assume that at least one value has been recorded.
    synchronized (this.usDataDerivLock) {
      value = this.usDataDeriv[(this.usIndex - 1 + US_DATA_SIZE) % US_DATA_SIZE];
    }
    return value;
  }

  /**
   * Get the moving statistics of the color sensor data.
   *
   * @return a float array holding the average, variance, and standard deviation of the color sensor data
   */
  public float[] getLLStats() {
    float[] stats = null;
    if (this.llFilled) {
      synchronized (this.llStatsLock) {
        stats = this.llStats.clone();
      }
    }
    return stats;
  }

  /**
   * Get the moving statistics of the ultrasonic sensor data.
   *
   * @return a float array holding the average, variance, and standard deviation of the ultrasonic sensor data
   */
  public float[] getUSStats() {
    float[] stats = null;
    if (this.usFilled) {
      synchronized (this.usStatsLock) {
        stats = this.usStats.clone();
      }
    }
    return stats;
  }

  /**
   * Increment by one the number of external objects accessing the color sensor data.
   */
  public int incrementLLRefs() {
    int refs;
    synchronized (this.llRefsLock) {
      refs = this.llRefs + 1;
      this.llRefs = refs;
    }
    return refs;
  }

  /**
   * Increment by one the number of external objects accessing the ultrasonic sensor data.
   */
  public int incrementUSRefs() {
    int refs;
    synchronized (this.usRefsLock) {
      refs = this.usRefs + 1;
      this.usRefs = refs;
    }
    return refs;
  }

  /**
   * Decrement by one the number of external objects accessing the color sensor data.
   */
  public int decrementLLRefs() {
    int refs;
    synchronized (this.llRefsLock) {
      refs = this.llRefs - 1;
      this.llRefs = refs;
    }
    return refs;
  }

  /**
   * Decrement by one the number of external objects accessing the ultrasonic sensor data.
   */
  public int decrementUSRefs() {
    int refs;
    synchronized (this.usRefsLock) {
      refs = this.usRefs - 1;
      this.usRefs = refs;
    }
    return refs;
  }

  /**
   * Update the moving statistics, `stats`, of the data samples, `data`.
   *
   * @param stats the moving statistics (average, variance, standard deviation) of our data
   * @param data the sample data on which to compute the moving statistics
   * @param index the index of the data value to remove from the moving statistics
   * @param val the new value to add to the moving statistics
   */
  private void updateMovingStatistics(float[] stats, float[] data, int index, float val) {
    float old = data[index];

    float n = data.length;

    float oldAvg = stats[0];
    float oldVar = stats[1];
    float oldDev = stats[2];

    //
    // Reference:
    //
    // [See http://jonisalonen.com/2014/efficient-and-accurate-rolling-standard-deviation]
    //
    float newAvg = oldAvg + (val - old) / n;
    float newVar = oldVar + (val - old) * (val - newAvg + old - oldAvg) / (n - 1);
    float newDev = (float)Math.sqrt((double)newVar);

    stats[0] = newAvg;
    stats[1] = newVar;
    stats[2] = newDev;
  }
}
