package src;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
/* 
 * Author: To Kim Bao Pham
 */
public class DoctorOffice {
	private  int patient=30;  // variables to take inputs from cmd line
	private  int doctor=3;
	Semaphore register = new Semaphore(0);     // semaphore for the register process by the receptionist
	Semaphore start = new Semaphore(0);        // semaphore receptionist wait on when the patient enter the room.
	Semaphore nurses = new Semaphore(doctor);  // semaphore for the available nurses
	Semaphore[] doctors = new Semaphore[doctor]; // semaphore for each doctor's availability
	Semaphore mutex0 = new Semaphore(1);       // semaphore to access wait queue
	Semaphore mutex1 = new Semaphore(1);     // semaphore to access patient queue
	Semaphore p_ready = new Semaphore(0);    // use to signal patient is ready
	Semaphore[] enter = new Semaphore[patient]; // use for nurse to take patient into office 
	Semaphore[] coord = new Semaphore[doctor]; // use to make sure patient is in the room before doctor listens
	Semaphore listen = new Semaphore(0); // use to make sure the doctor listens before giving advice
	Semaphore[] finished = new Semaphore[patient]; // for the patient when finished
	Queue<Integer> wait = new LinkedList<>(); // queue for waiting at the receptionist
	Queue<Integer> pat = new LinkedList<>(); // queue for registered patitents
	int[] pnum = new int[doctor]; // stores patient ID
	int[] dnum = new int[patient]; // stores doctor ID
	
	class Patient implements Runnable {
		private  int pnumber;  // IDs
		private  int dnumber;
		Patient(int num){
			pnumber = num;
		}
		
		public void run() {
				try {
					mutex0.acquire();  
					wait.add(pnumber);
					System.out.println("Patient "+pnumber+" enters waiting room, waits for receptionist.");
					mutex0.release();
					start.release(); // signal receptionist
					register.acquire();
					System.out.println("Patient "+ pnumber+ " leaves the receptionist and sits in waiting room");
					p_ready.release(); // signal nurse
					nurses.acquire();
					enter[pnumber].acquire();
					System.out.println("Patient "+ pnumber+ " enters the doctor's office");
					dnumber = dnum[pnumber];
					coord[dnumber].release();  // signal doctor
					listen.acquire();
					System.out.println("Patient "+ pnumber+ " receives advice from doctor "+ dnumber);
					finished[pnumber].acquire();
					System.out.println("Patient "+ pnumber+ " leaves");

					
				}catch (InterruptedException e) {}
			
		}
		
		

	}
	class Receptionist implements Runnable {
		private  int pnumber;
		
		Receptionist(){}
		
		public void run() {
			while(true) {
				try {
					start.acquire();
					mutex0.acquire(); // get patient ID
					pnumber = wait.poll();
					System.out.println("Receptionist registers patient "+ pnumber);
					mutex0.release();
					mutex1.acquire();
					pat.add(pnumber); // register patient
					mutex1.release();
					register.release();

					
				}catch (InterruptedException e) {break;}
			}
		}
		
		

	}
	
	class Nurse implements Runnable {
		private int nurseID;
		private int pnumber;
		Nurse(int num){
			nurseID = num;
		}
		public void run() {
			while(true) {
				try {
					p_ready.acquire(); // check for patient and doctor ready
					doctors[nurseID].acquire();
					mutex1.acquire();
					pnumber = pat.poll(); // get patient ID
					mutex1.release();
					pnum[nurseID]=pnumber;
					dnum[pnumber]=nurseID;
					System.out.println("Nurse "+ nurseID+ " takes patient " + pnumber + " to doctor's office");
					enter[pnumber].release(); 
					nurses.release();
					
					
				}catch (InterruptedException e) {break;}
			}
			
		}
		
	}
	
	class Doctor implements Runnable {
		private int docID;
		private int pnumber;
		Doctor(int num){
			docID = num;
		}
		public void run() {
			while(true) {
				try {
					coord[docID].acquire(); // wait for patient
					pnumber = pnum[docID];
					System.out.println("Doctor "+ docID+ " listens to symtoms from patient " + pnumber);
					listen.release();  // signal patient
					finished[pnumber].release();
					doctors[docID].release();
				}catch (InterruptedException e) { break;}
			}
			
		}
		
	}
	public void simulate(int doct, int pati) {
		int i = 0;
		patient=pati;
		doctor = doct;
		for(i=0; i<patient; i++) {  //initialize semaphores
			finished[i]=new Semaphore(0);
		}
		for(i=0; i<patient; i++) {
			enter[i]=new Semaphore(0);
		}
		for(i=0; i<doctor; i++) {
			doctors[i]=new Semaphore(1);
		}
		for(i=0; i<doctor; i++) {
			coord[i]=new Semaphore(0);
		}
		System.out.println("Run with "+patient+" patients, "+doctor+" nurses, "+doctor+" doctors\n");
		
		Receptionist r = new Receptionist();  // create threads
		Thread recept = new Thread(r);
		recept.start();
		
		Nurse[] n = new Nurse[doctor];
		Thread[] nur = new Thread[doctor];
		for( i = 0; i < doctor; ++i ) 
	      {
			 n[i] = new Nurse(i);
	         nur[i] = new Thread( n[i] );
	         nur[i].start();
	      }
		
		Doctor[] d = new Doctor[doctor];
		Thread[] doc = new Thread[doctor];
		for( i = 0; i < doctor; ++i ) 
	      {
			 d[i] = new Doctor(i);
	         doc[i] = new Thread( d[i] );
	         doc[i].start();
	      }
		
		Patient[] p = new Patient[patient];
		Thread[] pat = new Thread[patient];
		for( i = 0; i < patient; ++i ) 
	      {
			 p[i] = new Patient(i);
	         pat[i] = new Thread( p[i] );
	         pat[i].start();
	      }
		
		for( i = 0; i < patient; i++) {   // join threads
			try {
			pat[i].join();
			}catch (InterruptedException e) {}
		}
		recept.interrupt();
		try {
			recept.join(1);
		}catch (InterruptedException e) {}
		
		for (i = 0; i < doctor; i++) {
            try {
                nur[i].interrupt();
                nur[i].join(1);
            } catch (InterruptedException e) {}
		}
		
		for (i = 0; i < doctor; i++) {
            try {
                doc[i].interrupt();
                doc[i].join(1);
            } catch (InterruptedException e) {}
		}
		
		System.exit(0);
	}
	
	public static void main(String[] args) {
		DoctorOffice d = new DoctorOffice();  // run program
		d.simulate(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
	}
}

