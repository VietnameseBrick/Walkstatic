package edu.ucsd.cse110.walkstatic.teammate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;

import edu.ucsd.cse110.walkstatic.store.StorageWatcher;
import edu.ucsd.cse110.walkstatic.store.TeammateRequestStore;

public class TeammateRequests implements TeammateRequestListener {

    private TeammateRequestStore store;
    private Set<TeammateRequest> requestHashSet;
    private List<TeammateRequestsListener> teammateRequestsListenerList;
    private Teammate user;

    public TeammateRequests(@NonNull TeammateRequestStore store, @NonNull Teammate user){
        this.store = store;
        this.requestHashSet = new HashSet<>();
        this.teammateRequestsListenerList = new ArrayList<>();
        this.user = user;
    }

    public void addRequest(TeammateRequest request){
        if(!this.requestHashSet.contains(request)){
            this.store.addRequest(request);
            this.requestHashSet.add(request);
        }
    }

    public List<TeammateRequest> getRequests(){
        return new ArrayList<>(this.requestHashSet);
    }

    public void addRequestsListener(TeammateRequestsListener teammateRequestsListener){
        this.teammateRequestsListenerList.add(teammateRequestsListener);
    }

    @Override
    public void onNewTeammateRequest(TeammateRequest request) {
        this.requestHashSet.add(request);
        this.notifyListeners();
    }

    @Override
    public void onTeammateRequestDeleted(TeammateRequest request) {
        this.requestHashSet.remove(request);
        this.notifyListeners();
    }

    private void notifyListeners(){
        this.teammateRequestsListenerList.forEach(teammateRequestsListener -> {
            teammateRequestsListener.teammateRequestsUpdated(this.getRequests());
        });
    }

    public boolean areThereNotifications() {
        for(TeammateRequest request : this.getRequests()){
            if(request.getTarget().getEmail().equals(this.user.getEmail())){
                return true;
            }
        }
        return false;
    }

    public void deleteTeammateRequest(TeammateRequest request){
        this.store.delete(request);
    }
}
