import React from 'react';
import './Card.css';

const Card = ({ children, className = '', dark = false }) => {
  return (
    <div className={`card ${dark ? 'card-dark' : ''} ${className}`}>
      {children}
    </div>
  );
};

export default Card;
