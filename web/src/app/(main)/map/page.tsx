import { Map as MapIcon } from "lucide-react";

export default function MapPage() {
  return (
    <div className="animate-in fade-in duration-500">
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">Global Mood Map</h1>
        <p className="text-gray-500">See how the world is feeling right now, completely anonymized.</p>
      </div>
      
      <div className="bg-white rounded-[2rem] shadow-sm border border-gray-100 overflow-hidden relative min-h-[500px]">
        {/* Placeholder for actual interactive map (e.g., Mapbox or custom SVG) */}
        <div className="absolute inset-0 bg-[#e5e9ec] flex flex-col items-center justify-center p-6 text-center">
          <MapIcon className="w-16 h-16 text-gray-400 mb-4" />
          <h3 className="text-2xl font-bold text-gray-700 mb-2">Map Loading...</h3>
          <p className="text-gray-500 max-w-md">The interactive globe will render here, displaying color-coded regions based on the community's emotional weather.</p>
        </div>
      </div>
    </div>
  );
}
