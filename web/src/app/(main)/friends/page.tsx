import { Users } from "lucide-react";

export default function FriendsPage() {
  return (
    <div className="animate-in fade-in duration-500">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-4xl font-bold mb-2">Friends</h1>
          <p className="text-gray-500">Check on the people you care about.</p>
        </div>
        <button className="bg-[var(--color-ok-black)] text-white px-4 py-2 rounded-full shadow-sm text-sm font-medium cursor-pointer hover:bg-gray-800">
          Add Friend
        </button>
      </div>
      
      <div className="bg-white p-12 rounded-[2rem] shadow-sm border border-gray-100 text-center flex flex-col items-center justify-center">
        <div className="w-20 h-20 bg-gray-50 rounded-full flex items-center justify-center mb-6 border border-gray-100">
          <Users className="w-10 h-10 text-gray-300" />
        </div>
        <h3 className="text-xl font-bold text-gray-900 mb-2">No friends added yet</h3>
        <p className="text-gray-500 mb-6 max-w-sm">Invite friends to see their daily mood check-ins and send them quiet nudges of support.</p>
        <button className="bg-[var(--color-ok-orange)] hover:bg-[var(--color-ok-orange-shade)] text-white px-6 py-3 rounded-xl font-medium transition-colors">
          Invite a Friend
        </button>
      </div>
    </div>
  );
}
