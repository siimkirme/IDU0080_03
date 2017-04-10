package ee.ttu.idu0080.raamatupood.types;

import java.io.Serializable;

public class Tellimuserida implements Serializable{
	private Toode toode;
	private int kogus;
	
	public Tellimuserida(Toode toode, int kogus) {
		this.toode = toode;
		this.kogus = kogus;
	}
	public Toode getToode() {
		return toode;
	}
	public void setToode(Toode toode) {
		this.toode = toode;
	}
	public int getKogus() {
		return kogus;
	}
	public void setKogus(int kogus) {
		this.kogus = kogus;
	}
}
