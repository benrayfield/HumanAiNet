/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;
import static mutable.util.Lg.lgErr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import immutable.util.ImmutableLinkedListNode;

public class Parallel{
	private Parallel(){}
	
	/** Not using ForkJoinPool cuz...
	TODO use many small tasks, and find an efficient way for after end of each task it looks for another,
	compared to how I found in CRBM that using 1 ForkJoinTask per pixel row was far too slow
	but using 4 rectangles (with 4 cpus) was about 2.5 times faster than 1 cpu.
	*/
	public static final List<Thread> threads = new ArrayList();
	
	static volatile ImmutableLinkedListNode<Runnable> tasks;
	
	static synchronized Runnable getNextTaskOrNull(){
		if(tasks == null){
			return null;
		}else{
			Runnable data = tasks.data;
			tasks = tasks.nextOrNull;
			return data;
		}
	}
	
	static synchronized void addTask(Runnable task){
		tasks = new ImmutableLinkedListNode(task, tasks);
	}
	
	//public static final ForkJoinPool cpus;
	static{ 
		int howManyCpus = Runtime.getRuntime().availableProcessors(); //CPUs or APUs etc
		for(int i=0; i<howManyCpus; i++){
			Thread t = new Thread(()->{
				while(true){
					//TODO get next task, do it, then look for another. If there are no tasks,
					//wait until one is added instead of polling.
					Runnable task = getNextTaskOrNull();
					
					if(task != null){
						task.run();
					}else{
						try{
							Thread.sleep(1L);
						}catch(InterruptedException e){}
					}
					
					/* TODO if(task != null){
						try{
							task.run();
						}catch(Throwable t){
							//Task runner's job is to run task until it ends. It ended. Job well done.
							//If task had wanted something to be done if throw, it should have caught.
						}
					}else{
						try{
							Thread.sleep(1L<<60); //until InterruptedException
						}catch(InterruptedException e){
							TODO
						}
					}*/
					
					/*FIXME Almost-always have as many threads not-blocking as howManyCpus.
					TODO Detect when a thread is blocking. Need that event, so can alloc more threads,
					and dealloc (or for a shorter time, Thread.sleep) threads when theres more active than CPUs/APUs.
					Do this using Thread.getState(). Do it in a (high priority) task that looks at all the other threads
					then sleeps a very short time (unless there are no tasks, then sleeps until next task is added.
					*/
				}
			});
			//lower priority than JSoundCard's thread. TODO merge those with a system of priority per task.
			t.setPriority(Thread.MAX_PRIORITY-2);
			threads.add(t);
		}
		for(Thread t : threads){
			t.start();
		}
	}
	
	/** Task runner's job is to run task until it ends. It ended. Job well done.
	If task had wanted something to be done if throw, it should have caught.
	*/
	static void runNoThrow(Runnable r){
		try{
			r.run();
		}catch(Throwable t){}
	}
	
	public static void forkAndForget(int size, IntConsumer func){
		for(int i=0; i<size; i++){
			final int I = i;
			addTask(()->runNoThrow(()->func.accept(I)));
		}
	}
	
	
	/*fixme i think i need to stop writing to shared array even if its not same part of that array,
	so parallel class needs to change to return an object.
	maybe during this change i'll implement java stream interfaces or something related to them.
	
	TODO https://www.reddit.com/r/javahelp/comments/7xkgq7/is_it_considered_a_bug_in_jvm_if_2_threads/
		1

		Is it considered a bug in JVM if 2 threads writing to 2 adjacent indexs of shared Object array without synchronized can fail to write like a cache error? (self.javahelp)

		submitted 2 minutes ago by BenRayfield

		I ask cuz in hardware, caching is normally done in blocks such as adjacent 128 bytes, so if 2 threads write into the same block, they could both read more than they were supposed to read and write the whole 128 bytes with just their part updated. I may be experiencing this but its hard to track down. I am not using synchronized cuz no 2 threads write the same array index, but I'm thinking probably I should be.

		    commenteditsharesavehidedeletensfwspoilerflaircrosspost
	*/
		    
	/** FIXME eclipse debugger says "oldWeight cannot be resolved to a variable",
	and the problem is in the deeper recursion of edlayNormByMaxRadius its param is null.
	Why is it null? Is it cuz oldWeight cant be seen in eclipse? Or is it a thread error in Parallel?
	This kind of problem started when changed some of the loops in RBM to use Parallel, so its certainly
	at least indirectly related to this Parallel.forkAndWait func.
	Probably I should be using synchronized on something. Or is LWJGL native dll
	causing it even though i'm not calling lwjgl funcs from RBM as I commentedout that temporarily?
	Or maybe I should avoid writing to the same ndim array, even though its diff parts of that array
	that no 2 threads write the same part of, but since caching is written in blocks
	such as 128 bytes, it still could be that they are both writing the same part of the same array
	such as thread 1 writes index 1 and thread 2 writes index 2,
	but actually both of them read both and wrote both, and the other index was null when it was read
	and that null was written back.
	
	
	RBM.java: public static float[][][] edlayNormByMaxRadius(boolean gpu, final float[][][] oldWeight, float maxAllowedRadius){
		if(gpu){
			throw new Error("TODO");
		}else{
			lgErr("TODO gpu edlayNormByMaxRadius, then remove this message and just default to gpu.");
			float[][][] newWeight = new float[oldWeight.length][][];
			//for(int edlay=0; edlay<newWeight.length; edlay++){
			Parallel.forkAndWait(newWeight.length, (int edlay)->{
				newWeight[edlay] = edlayNormByMaxRadius(oldWeight[edlay], maxAllowedRadius);
			});
			//}
			return newWeight;
		}
	}
	*/
	public static synchronized void forkAndWait(int size, IntConsumer func){
		AtomicInteger myTasksDone = new AtomicInteger();
		final Thread callerThread = Thread.currentThread();
		for(int i=0; i<size; i++){
			final int I = i;
			addTask(()->{
				runNoThrow(()->func.accept(I));
				int d = myTasksDone.incrementAndGet();
				if(d == size){
					callerThread.interrupt();
				}
			});
		}
		int d = myTasksDone.get();
		if(d != size){
			try{
				Thread.sleep(1L<<60); //forever
			}catch(InterruptedException e){} //wait for InterruptedException when last task ends
		}
	}
	
	public static void forkAndForget(Runnable... funcs){
		forkAndForget(funcs.length, (int i)->funcs[i].run());
	}
	
	public static void forkAndWait(Runnable... funcs){
		forkAndWait(funcs.length, (int i)->funcs[i].run());
	}
	
	/** TODO implement this in various plugins (such as Aparapi or using OpenCL directly through JNI),
	else forkAndWait (CPU threads) if no such plugins available.
	TODO This is used in the 2 main bottlenecks of contrastiveDivergence (on multiple input vectors at once):
	Inference, and updating the weights when given the set of node states.
	All arrays are used as immutable.
	*/
	public static float[][] matrixMultiply(float[][] ab, float[][] bc){
		throw new Error("TODO");
	}

}