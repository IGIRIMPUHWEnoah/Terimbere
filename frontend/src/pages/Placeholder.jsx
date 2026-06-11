import Card from '../components/ui/Card';

const Placeholder = ({ title, description }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 600, color: 'var(--text-primary)' }}>{title}</h2>
      </div>
      <Card>
        <div style={{ textAlign: 'center', padding: '4rem 2rem', color: 'var(--text-secondary)' }}>
          <h3 style={{ fontSize: '1.5rem', marginBottom: '1rem', color: 'var(--primary-color)' }}>Coming Soon</h3>
          <p style={{ maxWidth: '400px', margin: '0 auto', lineHeight: '1.6' }}>
            {description || `The ${title} module is currently under development. Please check back later!`}
          </p>
        </div>
      </Card>
    </div>
  );
};

export default Placeholder;
