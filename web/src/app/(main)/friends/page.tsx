"use client";

import { useEffect, useState } from "react";
import { Users, Search, UserPlus, Check, Clock, ShieldCheck, HeartHandshake } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  searchUsers, 
  sendFriendRequest, 
  getFriendRequests, 
  acceptFriendRequest, 
  getFriendsWithMoods, 
  UserProfile, 
  Checkin 
} from "../../../utils/db";

export default function FriendsPage() {
  const [friends, setFriends] = useState<{ user: UserProfile; lastCheckin?: Checkin }[]>([]);
  const [requests, setRequests] = useState<{ id: string; user: UserProfile }[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<UserProfile[]>([]);
  const [sentRequests, setSentRequests] = useState<string[]>([]); // User IDs of sent requests
  const [activeTab, setActiveTab] = useState<'my-friends' | 'find-friends'>('my-friends');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    const friendsData = await getFriendsWithMoods();
    const requestsData = await getFriendRequests();
    setFriends(friendsData);
    setRequests(requestsData);
    setLoading(false);
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    const results = await searchUsers(searchQuery);
    setSearchResults(results);
  };

  // Run search on key up
  const handleQueryChange = async (val: string) => {
    setSearchQuery(val);
    if (val.trim().length >= 2) {
      const results = await searchUsers(val);
      setSearchResults(results);
    } else {
      setSearchResults([]);
    }
  };

  const handleAddFriend = async (targetUserId: string) => {
    const success = await sendFriendRequest(targetUserId);
    if (success) {
      setSentRequests([...sentRequests, targetUserId]);
    }
  };

  const handleAcceptRequest = async (requestId: string) => {
    const success = await acceptFriendRequest(requestId);
    if (success) {
      // Reload friends list and requests list
      const friendsData = await getFriendsWithMoods();
      const requestsData = await getFriendRequests();
      setFriends(friendsData);
      setRequests(requestsData);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="w-full max-w-4xl mx-auto"
    >
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-4xl font-bold mb-2 tracking-tight text-[var(--color-ok-black)]">Friends</h1>
          <p className="text-gray-500 text-lg">Check on the people you care about.</p>
        </div>

        {/* Tab Selector */}
        <div className="flex bg-white/60 backdrop-blur-md p-1 rounded-2xl border border-gray-100/50 shadow-sm self-start">
          <button
            onClick={() => setActiveTab('my-friends')}
            className={`px-5 py-2.5 rounded-xl text-sm font-semibold transition-all cursor-pointer ${
              activeTab === 'my-friends' 
                ? 'bg-[var(--color-ok-black)] text-white shadow-sm' 
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            My Friends
            {requests.length > 0 && (
              <span className="ml-2 bg-[var(--color-ok-orange)] text-white text-xs px-2 py-0.5 rounded-full">
                {requests.length}
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveTab('find-friends')}
            className={`px-5 py-2.5 rounded-xl text-sm font-semibold transition-all cursor-pointer ${
              activeTab === 'find-friends' 
                ? 'bg-[var(--color-ok-black)] text-white shadow-sm' 
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Find Friends
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400">
          <div className="w-12 h-12 border-4 border-[var(--color-ok-orange)] border-t-transparent rounded-full animate-spin mb-4"></div>
          <p className="text-lg">Loading social circle...</p>
        </div>
      ) : activeTab === 'my-friends' ? (
        <div className="space-y-8">
          {/* Incoming Friend Requests Section */}
          {requests.length > 0 && (
            <div className="bg-white/80 backdrop-blur-md p-6 rounded-[2.5rem] border border-[var(--color-ok-orange)]/10 shadow-sm">
              <h2 className="text-xl font-bold mb-4 text-[var(--color-ok-orange-shade)] flex items-center gap-2">
                <HeartHandshake className="w-5 h-5" />
                Friend Requests ({requests.length})
              </h2>
              <div className="grid sm:grid-cols-2 gap-4">
                {requests.map((req) => (
                  <div key={req.id} className="flex items-center justify-between p-4 bg-white rounded-2xl border border-gray-100 shadow-sm">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center border border-gray-100 text-lg">
                        {req.user.avatar_emoji || '🌙'}
                      </div>
                      <span className="font-bold text-gray-800">@{req.user.username}</span>
                    </div>
                    <button
                      onClick={() => handleAcceptRequest(req.id)}
                      className="bg-[var(--color-ok-teal)] hover:bg-[var(--color-ok-teal)]/90 text-white px-4 py-2 rounded-xl text-sm font-semibold flex items-center gap-1.5 transition-all cursor-pointer hover:scale-105 active:scale-95"
                    >
                      <Check className="w-4 h-4" /> Accept
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Friends List */}
          {friends.length === 0 ? (
            <div className="bg-white/80 backdrop-blur-md p-16 rounded-[2.5rem] border border-gray-100/50 shadow-sm text-center flex flex-col items-center justify-center">
              <div className="w-20 h-20 bg-gray-50 rounded-full flex items-center justify-center mb-6 border border-gray-100">
                <Users className="w-10 h-10 text-gray-300" />
              </div>
              <h3 className="text-xl font-bold text-gray-950 mb-2">No friends added yet</h3>
              <p className="text-gray-500 mb-6 max-w-sm">Invite friends to see their daily mood check-ins and send them quiet nudges of support.</p>
              <button 
                onClick={() => setActiveTab('find-friends')}
                className="bg-[var(--color-ok-orange)] hover:bg-[var(--color-ok-orange-shade)] text-white px-6 py-3 rounded-xl font-semibold transition-all hover:scale-105 active:scale-95 cursor-pointer shadow-md"
              >
                Find & Add Friends
              </button>
            </div>
          ) : (
            <div className="grid md:grid-cols-2 gap-6">
              {friends.map(({ user, lastCheckin }) => (
                <div 
                  key={user.id} 
                  className="bg-white/90 backdrop-blur-md p-6 rounded-[2.5rem] border border-gray-100 shadow-sm flex flex-col justify-between hover:shadow-md transition-shadow"
                >
                  <div className="flex items-start justify-between gap-4 mb-4">
                    <div className="flex items-center gap-3">
                      <div className="w-14 h-14 rounded-full bg-gray-50 flex items-center justify-center border border-gray-100 shadow-inner text-2xl">
                        {user.avatar_emoji || '👤'}
                      </div>
                      <div>
                        <h4 className="font-bold text-gray-900 text-lg">@{user.username}</h4>
                        <div className="text-xs text-gray-400">Added Friend</div>
                      </div>
                    </div>

                    {/* Check-in Mood Badge */}
                    {lastCheckin ? (
                      <div className="w-10 h-10 rounded-full flex items-center justify-center border shadow-sm text-xl bg-white select-none">
                        {lastCheckin.mood === 'good' ? '😎' : lastCheckin.mood === 'bad' ? '😔' : '🤔'}
                      </div>
                    ) : (
                      <div className="text-xs font-semibold px-2.5 py-1.5 rounded-full bg-gray-100 text-gray-400 flex items-center gap-1">
                        <Clock className="w-3 h-3" /> Waiting...
                      </div>
                    )}
                  </div>

                  <div className="bg-gray-50/70 border border-gray-100 p-4 rounded-2xl flex-1 flex flex-col justify-center min-h-[80px]">
                    {lastCheckin ? (
                      <div>
                        <div className="text-xs text-gray-400 font-bold mb-1 uppercase tracking-wider">Today's note:</div>
                        <p className="text-gray-700 italic font-medium">
                          {lastCheckin.note ? `"${lastCheckin.note}"` : "Checked in silently."}
                        </p>
                      </div>
                    ) : (
                      <p className="text-gray-400 text-center text-sm italic">
                        No check-in posted for today yet.
                      </p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        /* Find Friends Section */
        <div className="bg-white/80 backdrop-blur-md p-8 rounded-[2.5rem] border border-gray-100/50 shadow-sm space-y-6">
          <h2 className="text-2xl font-bold text-gray-900">Find new connections</h2>
          
          <form onSubmit={handleSearch} className="flex gap-2">
            <div className="flex-1 relative">
              <Search className="w-5 h-5 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search usernames..."
                value={searchQuery}
                onChange={(e) => handleQueryChange(e.target.value)}
                className="w-full pl-12 pr-4 py-3.5 rounded-2xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-ok-orange)] text-black bg-white"
              />
            </div>
            <button 
              type="submit"
              className="bg-[var(--color-ok-black)] hover:bg-gray-800 text-white px-6 rounded-2xl font-semibold transition-all cursor-pointer shadow-md"
            >
              Search
            </button>
          </form>

          <div className="space-y-3 pt-2">
            {searchResults.length > 0 ? (
              searchResults.map((usr) => {
                const alreadySent = sentRequests.includes(usr.id);
                const isFriend = friends.some(f => f.user.id === usr.id);
                return (
                  <div key={usr.id} className="flex items-center justify-between p-4 bg-white/70 rounded-2xl border border-gray-100 shadow-sm transition-all hover:bg-white">
                    <div className="flex items-center gap-3">
                      <div className="w-11 h-11 rounded-full bg-gray-50 flex items-center justify-center border border-gray-100 text-xl shadow-inner">
                        {usr.avatar_emoji || '🌙'}
                      </div>
                      <div>
                        <span className="font-bold text-gray-900 block">@{usr.username}</span>
                        {isFriend && <span className="text-[10px] text-teal-600 bg-teal-50 px-1.5 py-0.5 rounded-full font-bold">Friend</span>}
                      </div>
                    </div>

                    {isFriend ? (
                      <span className="text-sm font-semibold text-gray-400 flex items-center gap-1 px-4 py-2">
                        <ShieldCheck className="w-4 h-4 text-teal-500" /> Friend
                      </span>
                    ) : alreadySent ? (
                      <button
                        disabled
                        className="bg-gray-100 text-gray-400 px-4 py-2.5 rounded-xl text-sm font-semibold flex items-center gap-1.5 cursor-not-allowed"
                      >
                        <Clock className="w-4 h-4" /> Requested
                      </button>
                    ) : (
                      <button
                        onClick={() => handleAddFriend(usr.id)}
                        className="bg-[var(--color-ok-orange)] hover:bg-[var(--color-ok-orange-shade)] text-white px-4 py-2.5 rounded-xl text-sm font-semibold flex items-center gap-1.5 transition-all cursor-pointer hover:scale-105 active:scale-95 shadow-sm"
                      >
                        <UserPlus className="w-4 h-4" /> Add Friend
                      </button>
                    )}
                  </div>
                );
              })
            ) : searchQuery.trim().length >= 2 ? (
              <p className="text-center text-gray-400 py-6">No users found matching "{searchQuery}"</p>
            ) : (
              <div className="text-center text-gray-400 py-10 flex flex-col items-center">
                <Search className="w-10 h-10 text-gray-200 mb-2" />
                <p className="text-sm font-medium">Type a username to start searching</p>
              </div>
            )}
          </div>
        </div>
      )}
    </motion.div>
  );
}
