import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, StyleSheet, Platform } from 'react-native';

// import { useTheme } from '../../context/ThemeContext'; // Example context import

const NewComponent = ({ prop1, prop2 }) => {
  // const theme = useTheme(); // Example theme usage

  // 1. State declarations
  const [state, setState] = useState(null);

  // 2. Store hooks (if using a state management library)
  // const { data, updateData } = useAppStore();

  // 3. Other hooks
  useEffect(() => {
    // Effect logic here
  }, []);

  // 4. Callbacks
  const handlePress = useCallback(() => {
    // Callback logic here
  }, []);

  // 5. Render
  return (
    <View style={styles.container}>
      <Text style={styles.text}>NewComponent</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  text: {
    // Example of platform-specific styling
    ...Platform.select({
      ios: {
        fontSize: 18,
      },
      android: {
        fontSize: 16,
      },
      web: {
        fontSize: 20,
      },
    }),
  },
});

export default NewComponent;
