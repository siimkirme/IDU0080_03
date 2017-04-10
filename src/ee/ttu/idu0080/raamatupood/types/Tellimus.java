package ee.ttu.idu0080.raamatupood.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Tellimus implements Serializable{
	private List<Tellimuserida> tellimuseRead;
	
	public Tellimus() {
		tellimuseRead = new ArrayList<Tellimuserida>();
	}
	
	public void addTellimuseRida(Tellimuserida tellimuseRida) {
		tellimuseRead.add(tellimuseRida);
	}

	public List<Tellimuserida> getTellimuseRead() {
		return tellimuseRead;
	}
}
