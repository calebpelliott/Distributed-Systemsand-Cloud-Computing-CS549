package edu.stevens.cs549.dhts.state;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;

import edu.stevens.cs549.dhts.activity.DHTBase;
import edu.stevens.cs549.dhts.activity.IDHTNode;
import edu.stevens.cs549.dhts.activity.NodeInfo;
import edu.stevens.cs549.dhts.resource.TableRep;
import edu.stevens.cs549.dhts.resource.TableRow;

/**
 * 
 * @author dduggan
 */
public class State implements IState, IRouting {

	static final long serialVersionUID = 0L;

	public static Logger log = Logger.getLogger(State.class.getCanonicalName());
	
	protected NodeInfo info;

	public State(NodeInfo info) {
		super();
		this.info = info;
		this.predecessor = null;
		this.successor = info;

		this.finger = new NodeInfo[NKEYS];
		for (int i = 0; i < NKEYS; i++) {
			finger[i] = info;
		}

	}

	/*
	 * Get the info for this DHT node.
	 */
	public NodeInfo getNodeInfo() {
		return info;
	}

	/*
	 * Local table operations.
	 */
	private Persist.Table dict = Persist.newTable();

	@SuppressWarnings("unused")
	private Persist.Table backup = Persist.newTable();

	@SuppressWarnings("unused")
	private NodeInfo backupSucc = null;

	public synchronized String[] get(String k) {
		List<String> vl = dict.get(k);
		if (vl == null) {
			return null;
		} else {
			String[] va = new String[vl.size()];
			return vl.toArray(va);
		}
	}

	public synchronized void add(String k, String v) {
		List<String> vl = dict.get(k);
		if (vl == null) {
			vl = new ArrayList<String>();
			dict.put(k, vl);
		}
		vl.add(v);
		// TODO: broadcast an event to any listeners
		this.broadcastAddition(k, v);
	}

	public synchronized void delete(String k, String v) {
		List<String> vs = dict.get(k);
		if (vs != null)
			vs.remove(v);
	}

	public synchronized void clear() {
		dict.clear();
	}

	/*
	 * Operations for transferring state between predecessor and successor.
	 */

	/*
	 * Successor: Extract the bindings from the successor node.
	 */
	public synchronized TableRep extractBindings(int predId) {
		TableRep tr = Persist.extractBindings(predId, info, successor, dict);
		broadcastBindingChange(tr.entry, predecessor);
		return tr;
	}

	public synchronized TableRep extractBindings() {
		return Persist.extractBindings(info, successor, dict);
	}

	/*
	 * Successor: Drop the bindings that are transferred to the predecessor.
	 */
	public synchronized void dropBindings(int predId) {
		Persist.dropBindings(dict, predId, getNodeInfo().id);
		//broadcastBindingChange(dict);
	}

	/*
	 * Predecessor: Install the transferred bindings.
	 */
	public synchronized void installBindings(TableRep db) {
		dict = Persist.installBindings(dict, db);
	}

	/*
	 * Predecessor: Back up bindings from the successor.
	 */
	public synchronized void backupBindings(TableRep db) {
		backup = Persist.backupBindings(db);
		// backupSucc = db.getSucc();
	}

	public synchronized void backupSucc(TableRep db) {
		backupSucc = db.getSucc();
	}

	/*
	 * A never-used operation for storing state in a file.
	 */
	public synchronized void backup(String filename) throws IOException {
		Persist.save(info, successor, dict, filename);
	}

	public synchronized void reload(String filename) throws IOException {
		dict = Persist.load(filename);
	}

	public synchronized void display() {
		PrintWriter wr = new PrintWriter(System.out);
		Persist.display(dict, wr);
	}

	/*
	 * Routing operations.
	 */

	private NodeInfo predecessor = null;
	private NodeInfo successor = null;

	private NodeInfo[] finger;

	public synchronized void setPred(NodeInfo pred) {
		predecessor = pred;
	}

	public NodeInfo getPred() {
		return predecessor;
	}

	public synchronized void setSucc(NodeInfo succ) {
		successor = succ;
	}

	public NodeInfo getSucc() {
		return successor;
	}

	public synchronized void setFinger(int i, NodeInfo info) {
		/*
		 * Set the ith finger.
		 */
		finger[i] = info;
	}

	public synchronized NodeInfo getFinger(int i) {
		/*
		 * Get the ith finger.
		 */
		return finger[i];
	}

	public synchronized NodeInfo closestPrecedingFinger(int id) {
		/*
		 * Get closest preceding finger for id, to continue search at that
		 * node.
		 */
		for (int i = 0; i < IRouting.NFINGERS; i++) {
			if (DHTBase.inInterval(id, finger[i].id, finger[(i + 1) % (IRouting.NFINGERS)].id)) {
				return getFinger(i);
			}
		}
		
		return getFinger(IRouting.NFINGERS - 1);
	}

	public synchronized void routes() {
		PrintWriter wr = new PrintWriter(System.out);
		wr.println("Predecessor: " + predecessor);
		wr.println("Successor  : " + successor);
		wr.println("Fingers:");
		wr.printf("%7s  %3s  %s\n", "Formula", "Key", "Succ");
		wr.printf("%7s  %3s  %s\n", "-------", "---", "----");
		for (int i = 0, exp = 1; i < IRouting.NFINGERS; i++, exp = 2 * exp) {
			wr.printf(" %2d+2^%1d  %3d  [id=%2d,uri=%s]%n", info.id, i, (info.id + exp) % IRouting.NKEYS, finger[i].id,
					finger[i].addr);
		}
		wr.flush();
	}
	
	
	/*
	 * Used to prevent a race condition in the join protocol.
	 */
	
	public static enum JoinState {
		NOT_JOINED,
		JOINING,
		JOINED
	}
	
	private JoinState joinState = JoinState.NOT_JOINED;
	
	private Lock joinStateLock = new ReentrantLock();
	
	private Condition joined = joinStateLock.newCondition();
	
	public void startJoin() {
		joinStateLock.lock();
		try {
			joinState = JoinState.JOINING;
		} finally {
			joinStateLock.unlock();
		}
	}
	
	public void joinCheck() {
		// Called by any operations that should block during join protocol.
		// Currently that is getPred() (for the case where we are joining a 
		// single-node network).
		joinStateLock.lock();
		try {
			while (joinState == JoinState.JOINING) {
				joined.await();
			}
		} catch (InterruptedException e) {
			log.info("Join check loop was interrupted.");
		} finally {
			joinStateLock.unlock();
		}
	}
	
	public void finishJoin() {
		joinStateLock.lock();
		try {
			joinState = JoinState.JOINED;
			joined.signalAll();
		} finally {
			joinStateLock.unlock();
		}
	}
	

	/*
	 * Server-side listeners for new bindings.
	 */
	//          key
	private Map<String,SseBroadcaster> listeners = new HashMap<String,SseBroadcaster>();
	
	//          client id.  key.   event.
	private Map<Integer,Map<String,EventOutput>> outputs = new HashMap<Integer,Map<String,EventOutput>>();
	
	public void removeListener(int id, String key) {
		SseBroadcaster broadcaster = listeners.get(key);
		EventOutput eo = outputs.get(id).get(key);
		if((broadcaster == null) || (eo == null)) {
			return;
		}
		
		broadcaster.remove(outputs.get(id).get(key));
		
		/*try {
			eo.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		outputs.remove(id);*/
	}
	
	public void addListener(int id, String key, EventOutput os) {
		//TODO fix logic
		SseBroadcaster broadcaster = listeners.get(key);
		if(broadcaster == null) {
			broadcaster = new SseBroadcaster();
			listeners.put(key, broadcaster);
		}
		broadcaster.add(os);
		
		Map<String,EventOutput> m = outputs.get(id);
		if(m == null) {
			outputs.put(id, (Map<String, EventOutput>) new HashMap<String,EventOutput>());
			outputs.get(id).put(key, os);
		}
		else {
			m.put(key, os);
		}
		
		//m.put(key, os);
		//outputs.put(id, m);
	}
	
	private void broadcastAddition(String key, String value) {
		// TODO broadcast an added binding (use IDHTNode.NEW_BINDING_EVENT for event name).		
		final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
		eventBuilder.name(IDHTNode.NEW_BINDING_EVENT);
		eventBuilder.data(String.class, value);
		final OutboundEvent event = eventBuilder.build();
		
		SseBroadcaster broadcaster = listeners.get(key);
		if(broadcaster == null) {
			return;
		}
		broadcaster.broadcast(event);
	}
	
	private void broadcastBindingChange(TableRow[] entry, NodeInfo predecessor) {
		for(TableRow table : entry) {
			//Check if broadcaster exists
			SseBroadcaster broadcaster = listeners.get(table.key);
			if(broadcaster != null) {
				final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
				eventBuilder.name(IDHTNode.BINDING_MOVED_EVENT);
				eventBuilder.data(String.class, Integer.toString(predecessor.id) + "," + predecessor.addr);
				final OutboundEvent event = eventBuilder.build();
				broadcaster.broadcast(event);
				broadcaster.closeAll();
				listeners.remove(table.key);
			}
		}
	}
	
	/*
	 * Client-side callbacks for new binding notifications.
	 */
	
	private Map<String,EventSource> callbacks = new HashMap<String,EventSource>();
	
	public void addCallback(String key, EventSource is) {
		removeCallback(key);
		//is.open();
		callbacks.put(key, is);
	}
	
	public void removeCallback(String key) {
		// TODO remove an existing callback (if any) for bindings on key.
		// Be sure to close the event stream from the broadcaster.
		EventSource es = callbacks.get(key);
		if(es != null)
		{
			es.close();
			callbacks.remove(key);
		}
	}
	
	public void listCallbacks() {
		PrintWriter wr = new PrintWriter(System.out);
		if (callbacks.isEmpty()) {
			wr.println("No listeners defined.");
		} else {
			wr.println("Listeners defined for:");
			for (Entry<String, EventSource> entry : callbacks.entrySet()) {
				if (entry.getValue().isOpen()) {
					wr.println("  " + entry.getKey());
				} else {
					wr.println("  " + entry.getKey() + " (closed)");
					callbacks.remove(entry.getKey());
				}
			}
		}
		wr.flush();
	}


	
}
