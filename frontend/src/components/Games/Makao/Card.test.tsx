import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Card from './Card';
import { Card as CardType } from './types';

// Mock framer-motion to avoid animation issues in tests
jest.mock('framer-motion', () => ({
  motion: {
    div: ({ children, onClick, className, style, whileHover, ...props }: any) => (
      <div onClick={onClick} className={className} style={style} {...props}>
        {children}
      </div>
    ),
    img: ({ src, alt, className, style, ...props }: any) => (
      <img src={src} alt={alt} className={className} style={style} {...props} />
    ),
  },
}));

const mockCard: CardType = {
  rank: 'A',
  suit: 'hearts',
  id: '1'
};

describe('Card Component', () => {
  test('renders face down correctly', () => {
    render(<Card card={mockCard} isFaceDown={true} />);
    
    // Check for the "OG" text which appears when face down
    expect(screen.getByText('OG')).toBeInTheDocument();
    // Should not show rank/suit info
    expect(screen.queryByAltText(/ace of hearts/i)).not.toBeInTheDocument();
  });

  test('renders face up correctly', () => {
    render(<Card card={mockCard} isFaceDown={false} />);
    
    // Should show rank/suit info in alt text
    // The component logic: const rankName = getRankName(card.rank); // 'A' -> 'ace'
    // alt={`${card.rank} of ${card.suit}`} -> "A of hearts"
    const img = screen.getByAltText('A of hearts');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/SVG-cards-1.3/ace_of_hearts.svg');
  });

  test('calls onClick when clicked and playable', () => {
    const handleClick = jest.fn();
    render(<Card card={mockCard} isPlayable={true} onClick={handleClick} />);
    
    const cardElement = screen.getByAltText('A of hearts').closest('div');
    fireEvent.click(cardElement!);
    
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  test('does not call onClick when clicked and not playable', () => {
    const handleClick = jest.fn();
    render(<Card card={mockCard} isPlayable={false} onClick={handleClick} />);
    
    const cardElement = screen.getByAltText('A of hearts').closest('div');
    fireEvent.click(cardElement!);
    
    expect(handleClick).not.toHaveBeenCalled();
  });

  test('applies correct styles when playable', () => {
    render(<Card card={mockCard} isPlayable={true} />);
    
    const cardElement = screen.getByAltText('A of hearts').closest('div');
    expect(cardElement).toHaveClass('cursor-pointer');
    expect(cardElement).toHaveClass('ring-2');
  });

  test('applies correct styles when not playable', () => {
    render(<Card card={mockCard} isPlayable={false} />);
    
    const cardElement = screen.getByAltText('A of hearts').closest('div');
    expect(cardElement).toHaveClass('opacity-80');
    expect(cardElement).not.toHaveClass('ring-2');
  });
});
