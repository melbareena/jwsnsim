package application.appPI;

import hardware.Register;

public class LogicalClock {

	private Register value = new Register();

	public float rate = 0.0f;

	Register updateLocalTime = new Register();	

	public void update(Register local) {
		int timePassed = local.subtract(updateLocalTime).toInteger();
//		timePassed += (int) (((float) timePassed) * rate.getValue());
		timePassed += (int) (((float) timePassed) * rate);

		value = value.add(timePassed);
		this.updateLocalTime = new Register(local);
	}

	public Register getValue(Register local) {
		int timePassed = local.subtract(updateLocalTime).toInteger();
//		timePassed += (int) (((float) timePassed) * rate.getValue());
		timePassed += (int) (((float) timePassed) * rate);

		return value.add(new Register(timePassed));
	}

	public void setValue(Register time, Register local) {
		value = new Register(time);
		this.updateLocalTime = new Register(local);
	}
}
