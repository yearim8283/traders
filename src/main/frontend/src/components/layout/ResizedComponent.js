import React, { useState, useEffect } from 'react';


const ResizedComponent = ({ children }) => {
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  const handleResize = () => {
    setIsMobile(window.innerWidth <= 768);
  };

  useEffect(() => {
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  return (
    <div className={isMobile ? 'mobile-layout' : 'desktop-layout'}>
      {/* ... */}
      {children}
    </div>
  );
};

export default ResizedComponent;
